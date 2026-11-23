-- Copy these calls into ../ui.lua to restyle and hook the real Bellatrix taskbar.
-- Nothing here creates a second bar: every call edits the native views in place.

-- The shell itself: rounded fill, thinner padding, teal stroke.
nativeui:setBackground("id:taskBar", "taskbar", "#CC101418", 14)
nativeui:setPadding("id:taskBar", "taskbar", 4, 6, 4, 6)
nativeui:style("id:taskBar", "taskbar", {
    border = "#AA00AAA4",
    ["border-width"] = 1,
})

-- Dim the native bar while a pointer is down. The gesture does not consume the
-- touch, so taps and long-presses still reach Bellatrix's own taskbar actions.
nativeui:onGesture("id:taskBar", "taskbar", function(view, action, x, y)
    if action == "down" then
        view:alpha(0.85)
    elseif action == "up" or action == "cancel" then
        view:alpha(1.0)
    end
end)

-- A click hook observes the tap without replacing the launcher's click action.
nativeui:onClick("id:taskBar", "taskbar", function(view, action, x, y)
    nativeui:log("taskbar tapped at " .. math.floor(x) .. "," .. math.floor(y))
end)

-- Taskbar actions go through the same layout.toml the launcher itself writes.
nativeui:taskbar("pin", "UISettings")
nativeui:log("taskbar pins: " .. table.concat(nativeui:pins(), ", "))
