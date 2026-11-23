-- Copy these calls into ../ui.lua to change native Bellatrix Settings.
-- UISettingsCard is the launcher's own card view; no Settings screen is replaced.

-- Denser cards with a teal stroke and a two-value padding (vertical, horizontal).
nativeui:style("class:UISettingsCard", "settings", {
    background = "#202233",
    border = "#40FFFFFF",
    ["border-width"] = 1,
    radius = 8,
    padding = { 6, 8 },
})

-- Every native Settings TextView gets the same text color.
nativeui:setBackground("id:settings_content", "settings", "#FF0F1216", 0)
nativeui:setPadding("id:settings_content", "settings", 8, 8, 8, 8)

-- A long-press on a Settings card reports it. Bellatrix's own card click still runs.
nativeui:onLongClick("class:UISettingsCard", "settings", function(view, action, x, y)
    nativeui:log("settings card held: " .. view:id())
end)

-- Open the existing Settings page from a script; "1" is its System page, and
-- no new Activity is started.
nativeui:settings("1")
