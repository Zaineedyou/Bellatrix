-- Bellatrix native UI script.
--
-- This runs inside Bellatrix's one App.runtime. It only touches native
-- Views already on screen; it does not start another launcher, Activity,
-- home, drawer or Settings window. look.toml owns declarative style,
-- this file owns behaviour hooks and actions. README.md lists the API.
-- Guest path: ~/.config/bellatrix/native-ui/ui.lua
version = 1

-- Active examples are in examples/taskbar.lua and examples/settings.lua.
-- Copy what you need here, or call the API directly. For example:
--
-- nativeui:onGesture("id:taskBar", "taskbar", function(view, action, x, y)
--     if action == "down" then view:alpha(0.85)
--     elseif action == "up" or action == "cancel" then view:alpha(1.0) end
-- end)
--
-- nativeui:taskbar("pin", "UISettings")
-- nativeui:settings("1")
-- nativeui:setBackground("class:UISettingsCard", "settings", "#202233", 8)
