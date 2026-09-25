<# Saves the Minecraft window as a PNG for release QA (docs/qa-checklist.md). Brings the window to the front first if
   it isn't there (a minimize/restore plus a tap of Alt lets this process do that).
   Usage: powershell -File tools/qa/capture.ps1 build/qa/26.1-neoforge/front.png #>
param([Parameter(Mandatory)][string]$OutFile)

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class QaWindow {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr hWnd, int attribute, out RECT rect, int size);
}
"@

[QaWindow]::SetProcessDPIAware() | Out-Null
$game = Get-Process java, javaw -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowTitle -like "Minecraft*" } | Select-Object -First 1
if ($null -eq $game) { Write-Output "no Minecraft window"; exit 1 }
$hwnd = $game.MainWindowHandle
if ([QaWindow]::GetForegroundWindow() -ne $hwnd) {
    [QaWindow]::ShowWindow($hwnd, 6) | Out-Null
    [QaWindow]::ShowWindow($hwnd, 9) | Out-Null
    [QaWindow]::keybd_event(0x12, 0, 0, [UIntPtr]::Zero)
    [QaWindow]::keybd_event(0x12, 0, 2, [UIntPtr]::Zero)
    [QaWindow]::SetForegroundWindow($hwnd) | Out-Null
    Start-Sleep -Milliseconds 1500
}
# The window's visible frame (DWMWA_EXTENDED_FRAME_BOUNDS): GetWindowRect also counts an invisible border, which would
# capture a strip of whatever window is behind the game.
$rect = New-Object QaWindow+RECT
if ([QaWindow]::DwmGetWindowAttribute($hwnd, 9, [ref]$rect, 16) -ne 0) { [QaWindow]::GetWindowRect($hwnd, [ref]$rect) | Out-Null }
$bitmap = New-Object System.Drawing.Bitmap ($rect.Right - $rect.Left), ($rect.Bottom - $rect.Top)
[System.Drawing.Graphics]::FromImage($bitmap).CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
New-Item -ItemType Directory -Force (Split-Path -Parent $OutFile) | Out-Null
$bitmap.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output "saved $OutFile"
