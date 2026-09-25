<# Plays the game for release QA (docs/qa-checklist.md): picks a hotbar slot, sneaks, clicks, presses keys, or turns
   book pages, in that order. The game window must be in front (run capture.ps1 once first).
   Usage: powershell -File tools/qa/input.ps1 -Slot 1 -Sneak -Click right
          powershell -File tools/qa/input.ps1 -Keys "{ESC}"       (SendKeys syntax: "e", "{ESC}")
          powershell -File tools/qa/input.ps1 -PageDown 10        (Page Down; SendKeys' {PGDN} reaches the game as numpad 3) #>
param([int]$Slot = 0, [switch]$Sneak, [ValidateSet("", "left", "right")][string]$Click = "", [string]$Keys = "",
      [int]$PageDown = 0)

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class QaInput {
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, int dx, int dy, uint data, UIntPtr extra);
}
"@

if ($Slot -ge 1) {
    [QaInput]::keybd_event([byte](0x30 + $Slot), 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 100
    [QaInput]::keybd_event([byte](0x30 + $Slot), 0, 2, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 300
}
if ($Sneak) {
    [QaInput]::keybd_event(0xA0, 0x2A, 0, [UIntPtr]::Zero)   # left shift down
    Start-Sleep -Milliseconds 400                             # the server learns about sneaking a tick later
}
if ($Click -ne "") {
    $down, $up = if ($Click -eq "right") { 0x0008, 0x0010 } else { 0x0002, 0x0004 }
    [QaInput]::mouse_event($down, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [QaInput]::mouse_event($up, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 300
}
if ($Sneak) { [QaInput]::keybd_event(0xA0, 0x2A, 2, [UIntPtr]::Zero) }
if ($Keys -ne "") {
    [System.Windows.Forms.SendKeys]::SendWait($Keys)
    Start-Sleep -Milliseconds 300
}
for ($i = 0; $i -lt $PageDown; $i++) {
    [QaInput]::keybd_event(0x22, 0x51, 0x1, [UIntPtr]::Zero)   # VK_NEXT as an extended key: the real Page Down
    Start-Sleep -Milliseconds 80
    [QaInput]::keybd_event(0x22, 0x51, 0x3, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 150
}
Write-Output "done"
