/**
 *  Smart Dim Nightlight
 *
 *  A nightlight that will come on when motion is detected
 *  See more of a description below
 *
 *  Version 1.0.0 (2017-06-13)
 *
 * Heavily leveraged from Dim and Dimmer:
 *    <https://github.com/statusbits/smartthings/tree/master/DimAndDimmer/DimAndDimmer.groovy>
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2017 ckowalski-1
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

definition(
    name: "Dim and Dimmer",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "Smart Dim Nightlight",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true
)

preferences {
    page name:"pageSetup"
    page name:"pageAbout"
    page name:"pageConfigure"
}

// Show "Setup Menu" page
private def pageSetup() {
    TRACE("pageSetup()")

    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
        return pageAbout()
    }

    def inputDimmers = [
        name        : "dimmers",
        type        : "capability.switchLevel",
        title       : "Select Dimmers",
        multiple:   true,
        required:   false
    ]

    def inputSwitches = [
        name        : "switches",
        type        : "capability.switch",
        title       : "Select Switches",
        multiple:   true,
        required:   false
    ]

    def inputMode = [
        name        : "enableMode",
        type        : "location.mode",
        title       : "Select mode to enable (optional)",
        multiple:   false,
        required:   false
    ]
    
    def inputDoubleTap = [
        name        : "doubleTapSwitch",
        type        : "capability.switch",
        title       : "Select switch to enable/disable with double tap (optional)",
        multiple:   false,
        required:   false
    ]

    def pageProperties = [
        name        : "pageSetup",
        title       : "Setup Menu",
        nextPage    : "pageConfigure",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            input inputDimmers
            input inputSwitches
            input inputMode
            input inputDoubleTap
            href "pageAbout", title:"About", description:"Tap to open"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

// Show "About" page
private def pageAbout() {
    TRACE("pageAbout()")

    def textAbout =
        "This smart app allows you to create dimmable nightlight. " +
        "You configure an off level and an on level.  You can " +
        "set nightligt be come on based on a mode switch  or " +
        "double tap to turn it on or off"

    def pageProperties = [
        name        : "pageAbout",
        title       : "About",
        nextPage    : "pageSetup",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
            paragraph "${textVersion()}\n${textCopyright()}"
        }
        section("License") {
            paragraph textLicense()
        }
    }
}

// Show "Configure Dimmers and Switches" setup page
private def pageConfigure() {
    TRACE("pageConfigure()")

    def textAbout =
        "Set desired dimming levels for each switch. Dimming values " +
        "are between 0 (off) and 99 (full brightness). There is an on " +
        "value and an off value for each switch."

    def pageProperties = [
        name        : "pageConfigure",
        title       : "Configure Dimmers and Switches",
        nextPage    : null,
        install     : true,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }
        
        settings.dimmers?.each() {
            def name = it as String
            section("${name} Config", hideable:true, hidden:false) {
                name = name.tr(' !+', '___')
                settings.dimmers?.each() {
                    input "${it.id}_${name} On Value", "number", title:it.displayName, required:true
                }
                settings.dimmers?.each() {
                    input "${it.id}_${name} Off Value", "number", title:it.displayName, required:true
                }
            }
        }
    }
}

def installed() {
    TRACE("installed()")

    state.installed = true
    initialize()
}

def updated() {
    TRACE("updated()")

    unsubscribe()
    initialize()
}

// Handle location event.
def onLocation(evt) {
    TRACE("onLocation(${evt.value})")

    String mode = evt.value

    def allSwitches = []
    if (settings.dimmers)
        allSwitches.addAll(settings.dimmers)
    if (settings.switches)
        allSwitches.addAll(settings.switches)

    allSwitches.each() {
        def name = "${it.id}_${mode.tr(' !+', '___')}"
        TRACE("name: ${name}")
        def value = settings[name]
        TRACE("value: ${value}")
        if (value != null) {
            if (value == 'on') {
                TRACE("Turning '${it.displayName}' on")
                it.on()
            } else if (value == 'off') {
                TRACE("Turning '${it.displayName}' off")
                it.off()
            } else {
                value = value.toInteger()
                if (value > 99) value = 99
                TRACE("Setting '${it.displayName}' level to ${value}")
                it.setLevel(value)
            }
        }
    }
}

private def initialize() {
    log.trace "${app.name}. ${textVersion()}. ${textCopyright()}"

    subscribe(location, onLocation)
    STATE()
}

private def textVersion() {
    def text = "Version 1.0.0"
}

private def textCopyright() {
    def text = "Copyright (c) 2017 Christopher Kowalski"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def TRACE(message) {
    //log.debug message
}

private def STATE() {
    //log.trace "settings: ${settings}"
    //log.trace "state: ${state}"
}