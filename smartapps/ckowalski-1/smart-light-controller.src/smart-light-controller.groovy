/**
 *  Smart Light Controller
 *
 *  Copyright 2015 Christopher Kowalski
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smart Light Controller",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "Control when a light turns on and off with separate \"enabled\" and \"action\" conditions. For example a light might turn on when someone arrives (action) but only when it's dark \"enabled\".\r\n\r\nEnable choices are time and light level.\r\nAction choices are motion, open/close, and arrival.\r\n\r\nWhen turned on the light will remain on for a set number of minutes after all actions are cleared. For example if two different open/close sensors are used, the light won't turn off until after BOTH are closed.\r\n",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png")
    
    
preferences {
	page(name: "firstPage")
    page(name: "pageAbout")
//    page(name: "secondPage")
    page(name: "dimmerConfigPage")
    page(name: "installPage")
}

// First page
def firstPage() {
    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
        return pageAbout()
    }

	dynamicPage(name: "firstPage", title: "Select lights and triggers", nextPage: "dimmerConfigPage", install: false, uninstall: state.installed) {
        section("About") {
            href "pageAbout", title:"About", description:"Tap to open"
        }
        section("Control these lights (switches) ...") {
            input "lights", "capability.switch", multiple: true, required: false
        }
        section("Control these lights (dimmers) ...") {
            input "dimmers", "capability.switchLevel", multiple: true, required: false
        }
        section("Turning on if there's movement ..."){
            input "motionSensor", "capability.motionSensor", title: "Motion?", multiple: true, required: false
        }
        section("Or when a door opens ...") {
            input "doors", "capability.contactSensor", title: "Door?", multiple: true, required: false
        }
        section("Or when someone arrives") {
            input "people", "capability.presenceSensor", multiple: true, required:false
        }
        section("And then off when it's light, or there's been no movement, or all doors are closed for ..."){
            input "delayMinutes", "number", title: "Minutes?", defaultValue: 0
        }
        section("Persistent Mode?") {
        	input "perst", "bool", defaultValue: "true", required: false
        }
        section("Pick when this app will be enabled") {
            input("enableType", "enum", options: [
                "alwaysEnabledChoice":"Always Enabled",
                "lightSensorChoice":"Light Sensor",
                "modeEnableChoice":"Mode Change",
                "sunChoice": "Sun Rise/Set"], defaultValue: "alwaysEnabledChoice", submitOnChange: true)
		}
        
        // Options are based on enable type
        if (enableType == "alwaysEnabledChoice") {
            section("No additional setup for Always Enabled")

        } else if (enableType == "lightSensorChoice") {
            section("Pick a light sensor"){
                input "lightSensor", "capability.illuminanceMeasurement", required: true
                input "actionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes(), defaultValue: "Nothing"
                input "actionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes(), defaultValue: "OffFull"
            }

        } else if (enableType == "modeEnableChoice") {
            section("Pick the enable and disable modes"){
            input "enableMode", title: "Enable Mode", type: "location.mode", required: true, mutiple: false
            input "disableMode", title: "Disable Mode", type: "location.mode", required: true, mutiple: false
            input "actionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes(), defaultValue: "On"
            input "actionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes(), defaultValue: "OffFull"
            }

        // Sun set/rise
        } else if (enableType == "sunChoice") {
            section ("Sunrise offset (optional)...") {
                input "sunriseOffsetValue", "text", title: "HH:MM", required: false
                input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            }
            section ("Sunset offset (optional)...") {
                input "sunsetOffsetValue", "text", title: "HH:MM", required: false
                input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            }
            section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
                input "zipCode", "text", title: "Zip code", required: false
            }
            section ("What to do on enable and disable?") {
                input "actionOnEnable", "enum", title: "What should happne on enable", required: true, options: enableActionTypes(), defaultValue: "On"
                input "actionOndisable", "enum", title: "What should happen on disable", required: true, options: enableActionTypes(), defaultValue: "OffFull"
            }
        }
       
        section("Select switch to enable/disable automation with double tap (optional)") {
            input "doubleTapSwitch", "capability.switch", multiple: false, required: false, submitOnChange: true
		}
        
        // Options are presented if a switch is chosed
        if (doubleTapSwitch != null) {
            section("Pick what to do when automation is enabled or disabled via the double tap switch") {
                input "DTactionOnEnable", "enum", title: "What to do on enable (double tap on)", required: true, options: enableActionTypes(), defaultValue: "On"
                input "DTactionOndisable", "enum", title: "What to do on disable (double tap off)", required: true, options: enableActionTypes(), defaultValue: "OffFull"
            }
        }
        
        section("Select switch to enable/disable automation with on/off toggle (optional)") {
            input "toggleSwitch", "capability.switch", multiple: false, required: false, submitOnChange: true
		}
        
        // Options are presented if a switch is chosed
        if (toggleSwitch != null) {
            section("Pick what to do when automation is enabled or disabled via  the toggle switch") {
                input "STactionOnEnable", "enum", title: "What to do on enable (On then off)", required: true, options: enableActionTypes(), defaultValue: "On"
                input "STactionOndisable", "enum", title: "What to do on disable (off then on)", required: true, options: enableActionTypes(), defaultValue: "OffFull"
            }
        }
    }
}

private enableActionTypes() {
     return [
            "enableNothing":"Nothing",
            "enableOn":"On",
            "enableOnFull":"Full On",
            "enableOff": "Off",
            "enableOffFull": "Full Off"
            ]
}
/* // Page to setup based on Enabled Type
def secondPage() {
    dynamicPage(name: "secondPage", title: "Setup Enable Choice", nextPage: "thirdPage", install: false, uninstall: true) {
    	if (enableType == "alwaysEnabledChoice") {
            section("No Setup for Always Enabled")

		} else if (enableType == "lightSensorChoice") {
            section("Pick a light sensor"){
            input "lightSensor", "capability.illuminanceMeasurement", required: true}

		// Sun set/rise
        } else {
            section ("Sunrise offset (optional)...") {
            	input "sunriseOffsetValue", "text", title: "HH:MM", required: false
            	input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        	}
        	section ("Sunset offset (optional)...") {
            	input "sunsetOffsetValue", "text", title: "HH:MM", required: false
            	input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        	}
        	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
            	input "zipCode", "text", title: "Zip code", required: false
            }
        }
    }
}
*/

def installPage() {
	dynamicPage(name: "installPage", title: "Name app and configure modes", install: true, uninstall: state.installed) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

// Show "About" page
private def pageAbout() {

    def textAbout =
        "Control when a light turns on and off with separate \"enabled\" and \"action\" conditions. " +
        "For example a light might turn on when someone arrives (action) but only when it's dark (enabled)." +
        "\r\n\r\nEnable choices are time (based on sunrise/set), light level, always, or mode change.  The " +
        "automatation can also be optionally enabled/disabled by double tapping on or off a switch." +
        "\r\nAction choices are motion, open/close, arrival, or mode change." +
        "\r\n\r\nWhen turned on the light will remain on for a set number of minutes after all actions are cleared. " +
        "For example if two different open/close sensors are used, the light won't turn off until after BOTH are closed." +
        "\r\n\r\nPersistent Mode means the light will always be \"turned on\" on a action even it has been manually turned off. " +
        "If you disable presistent mode and manually turn off the light it won't turn back on until all actions clear for the configured time" +
        "\r\n\r\nYou can also control what happens when automation is enabled.  On means dimmers set to the on value, Full On means dimmers set to 100% " +
        "Likewise Off means dimmers set to the off value and Full Off means dimmers set to 0%"

    def pageProperties = [
        name        : "pageAbout",
        title       : "About",
        nextPage    : "firstPage",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }
    }
}

// Show "Configure Dimmers and Switches" setup page
private def dimmerConfigPage() {
    if (dimmers == null) {
        return installPage()
    }
    
    def textAbout =
        "Set desired dimming levels for each switch. Dimming values " +
        "are between 0 (off) and 99 (full brightness)."

    def pageProperties = [
        name        : "dimmerConfigPage",
        title       : "Configure Dimmers and Switches",
        nextPage    : installPage,
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }
        
        settings.dimmers?.each() {
            def name = it as String
            section("${name} Config", hideable:true, hidden:false) {
              input "${name}_OnVal", "number", title:"${name} On Value", required:true
              input "${name}_OffVal", "number", title:"${name} Off Value", required:true
            }
        }
    }
}


def installed() {
	initialize()
    state.installed = true
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	if (motionSensor) {
		subscribe(motionSensor, "motion", motionHandler)
	}
	if (doors) {
		subscribe(doors, "contact", contactHandler)
	}
    
    if (people) {
    	subscribe(people, "presence", presenceHandler)
    }
    
    // Config based on the enable type
	if (enableType == "alwaysEnabledChoice") {
    	state.alwaysEnabled = true
        state.enabled = true
        def lightSensor = null
    } else if (enableType == "lightSensorChoice") {
    	state.alwaysEnabled = false
        state.enabled = false
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	} else if (enableType == "modeEnableChoice") {
    	subscribe(location, modeHandler)
        state.enabled = false
    } else if (enableType == "sunChoice") {
    	state.alwaysEnabled = false
        state.enabled = false
        def lightSensor = null
        state.riseTime = 0
        state.setTime = 0
		subscribe(location, "position", locationPositionChange)
		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		astroCheck()
        // schedule an astro check every 1h to work around SmartThings missing scheduled events issues
        runEvery1Hour(astroCheck)
        // Set the initial enabled state (from astroCheck)
        state.enabled = state.astroEnabled
	}
    
    if (doubleTapSwitch) {
        subscribe(doubleTapSwitch, "switch", switchHandler, [filterEvents: false])
    }
    
    if (toggleSwitch) {
        subscribe(toggleSwitch, "switch", switchHandlerToggle, [filterEvents: false])
    }
    
    state.lastStatus = "unknown"
    log.debug "Initialize: Always Enabled: $state.alwaysEnabled"
    log.debug "Initialize: Light Sensor: $lightSensor"
    log.debug "Initialize: Persistent mode: $perst"
    log.debug "Initialize: enabled: $state.enabled"
    log.debug "Initialize: Action on Enable: $actionOnEnable"
    log.debug "Initialize: Action on Disable: $actionOndisable"
    log.debug "Initialize: DT Action on Enable: $DTactionOnEnable"
    log.debug "Initialize: DT Action on Disable: $DTactionOndisable"
    log.debug "Initialize: ST Action on Enable: $STactionOnEnable"
    log.debug "Initialize: ST Action on Disable: $STactionOndisable"
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}


//
// Check is there are any doors open
//
def checkDoorsOpen() {
	// Check for open doors if doors are used, else return false
	if (doors) {
        def listOfOpenDoors = doors.findAll { it?.latestValue("contact") == "open" }
        log.debug "Anything open $listOfOpenDoors"
        listOfOpenDoors ? true : false
    } else {
    	return false
    }
}

//
// Check is there is any motion
//
def checkAnyMotion() {
	// Check for motion if montion sensors are used, else return false
	if (motionSensor) {
        def listOfActiveMotion = motionSensor.findAll { it?.latestValue("motion") == "active" }
        log.debug "Anything moving $listOfActiveMotion"
        listOfActiveMotion ? true : false
    } else {
    	return false
    }
}


//
// Motion Handler
//
def motionHandler(evt) {
	def lastStatus = state.lastStatus
	log.debug "$evt.name: $evt.value"
    log.debug "MotionHandler: perst: $perst"
    log.debug "MotionHandler: lastStatus: $lastStatus"
	if (evt.value == "active") {
		if (enabled()) {
        	if (lastStatus != "on" || perst ) {
				log.debug "turning on lights due to motion"
				if (lights != null) {lights.on()}
                if (dimmers != null) {turnOnDimmers()}
				state.lastStatus = "on"
            }
		}
		state.motionStopTime = null
	}
	
	// No motion
	else {
    	// Only schedule if enabled.
        // When not enabled do not control the lights
    	if (enabled()) {
    		scheduleCheckTurnOff()
        }
	}
}


//
// Contact Handler
//
def contactHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "open") {
		if (enabled()) {
			log.debug "turning on lights due to door opened"
			if (dimmers != null) {lights.on()}
            if (lights != null) {turnOnDimmers()}
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	
	// Door Closed
	else {
    	// Only schedule if enabled.
        // When not enabled no control on lights
    	if (enabled()) {
    		scheduleCheckTurnOff()
        }
	}
}


//
// Light Handler
//
def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
    // Turn off enable
	if (evt.integerValue > 50 && state.enabled != false) {
        log.debug "Disable automation since it's bright"
		state.enabled = false
		enableChangeActions(actionOndisable)
	} else if (evt.integerValue < 30 && state.enabled != true) {
        log.debug "Enable automation since it's dark"
		state.enabled = true
		enableChangeActions(actionOnEnable)
    }
    
    // Check if it just got dark enough to turn on the lights
	if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
		log.debug "turning on lights since it's now dark and there is motion or something opened"
		if (lights != null) {lights.on()}
        if (dimmers != null) {turnOnDimmers()}
		state.lastStatus = "on"
        state.motionStopTime = null
	}
}

//
// Presence Handler
//
def presenceHandler(evt) {
	log.debug "$evt.name: $evt.value"
    // Don't do anything if the off time is 0 or we'd
    // just turn the light on then off
	if (evt.value == "present" && delayMinutes != 0) {
		if (enabled()) {
			log.debug "turning on lights due to presence"
			if (lights != null) {lights.on()}
            if (dimmers != null) {turnOnDimmers()}
			state.lastStatus = "on"
		}
		state.motionStopTime = null
        // Now see if an off event should be scheduled
        scheduleCheckTurnOff()
	}
}

//
// Handle location event.
//
def modeHandler(evt) {
    log.debug "modeHandler: $evt.value"

    // Enable Mode, turn on lights
    if (evt.value == enableMode && state.enabled != true) {
        state.enabled = true
        log.debug "modeHandler: Enable"
        enableChangeActions(actionOnEnable)
        if (checkDoorsOpen() || checkAnyMotion()) {
            if (lights != null) {lights.on()}
            if (dimmers != null) {turnOnDimmers()}
            state.lastStatus = "on"
            state.motionStopTime = null
        }
    // Disable mode
    } else if (evt.value == disableMode && state.enabled != false) {
        state.enabled = false
        log.debug "modeHandler: Disable"
        enableChangeActions(actionOndisable)
    }
}

//
// Sunset Handler
//  Check for any open doors to turn lights on if they were opened before
//
def sunsetHandler() {
	log.debug "Executing sunset handler"
    enableChangeActions(actionOnEnable)
    // Check if there is a door open to turn on the lights
	if (checkDoorsOpen() || checkAnyMotion()) {
		log.debug "turning on lights since it's now sunset and there is something opened"
		if (lights != null) {lights.on()}
        if (dimmers != null) {turnOnDimmers()}
		state.lastStatus = "on"
	}
    state.enabled = true
	state.motionStopTime = null
}
    	
//
// Sunrise Handler
//  Turn off lights at sunrise
//
def sunriseHandler() {
	log.debug "Executing sunrise handler"
    enableChangeActions(actionOndisable)
    state.enabled = false
}

//
// Switch Handler: for double tap
//
def switchHandler(evt) {
	log.debug "switchHandler: $evt.value , was physical: $evt.physical"

	// use Event rather than DeviceState because we may be changing DeviceState to only store changed values
	def recentStates = doubleTapSwitch.eventsSince(new Date(now() - 4000), [all:true, max: 10]).findAll{it.name == "switch"}
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

	if (evt.physical) {
		if (evt.value == "on" && lastTwoStatesWere("on", recentStates, evt, "on")) {
			log.debug "detected two taps, enable automation"
			state.enabled = true
            enableChangeActions(DTactionOnEnable)
		} else if (evt.value == "off" && lastTwoStatesWere("off", recentStates, evt, "off")) {
			log.debug "detected two taps, disable automation"
			state.enabled = false
            enableChangeActions(DTactionOndisable)
		}
	}
	else {
		log.debug "Skipping digital on/off event"
	}
}

//
// Switch Handler: for toggle
//
def switchHandlerToggle(evt) {
	log.debug "switchHandlerToggle: $evt.value"

	// use Event rather than DeviceState because we may be changing DeviceState to only store changed values
	def recentStates = toggleSwitch.eventsSince(new Date(now() - 10000), [all:true, max: 10]).findAll{it.name == "switch"}
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

    // Enable is On then off
    if (evt.value == "off" && lastTwoStatesWere("off", recentStates, evt, "on")) {
		log.debug "detected on then off, enable automation"
		state.enabled = true
        enableChangeActions(STactionOnEnable)
    //Disable is Off then On
	} else if (evt.value == "on" && lastTwoStatesWere("on", recentStates, evt, "off")) {
		log.debug "detected off then on, disable automation"
		state.enabled = false
        enableChangeActions(STactionOndisable)
	}
}


// Helper function to determine double tap or toggle
private lastTwoStatesWere(value, states, evt, value1) {
	def result = false
	if (states) {

		log.trace "unfiltered: [${states.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
		def onOff = states.findAll { it.physical || !it.type }
		log.trace "filtered:   [${onOff.collect{it.dateCreated + ':' + it.value}.join(', ')}]"

		// This test was needed before the change to use Event rather than DeviceState. It should never pass now.
		if (onOff[0].date.before(evt.date)) {
			log.warn "Last state does not reflect current event, evt.date: ${evt.dateCreated}, state.date: ${onOff[0].dateCreated}"
			result = evt.value == value && onOff[0].value == value
		}
		else {
			result = onOff.size() > 1 && onOff[0].value == value && onOff[1].value == value1
		}
    }
	result
}


//
// Schedule a time to run checkTurnOff()
//
def scheduleCheckTurnOff() {
	// Check if there is no motion and no doors open to schedule a time to check if lights should be turned off
	if (!checkDoorsOpen() && !checkAnyMotion()) {
		state.motionStopTime = now()
		if(delayMinutes) {
			runIn(delayMinutes*60, checkTurnOff)
            log.debug "Scheduling turn off"
		} else {
			checkTurnOff()
		}
	}
}


//
// Check if the light(s) should be turned off
//
def checkTurnOff() {
	log.debug "In checkTurnOff, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    // Check only if:
    	// - there is a stopTimeSet (this is set to Null when we turn on),
        // - All doors are closed and there is no motion
	if (state.motionStopTime && !checkDoorsOpen() && !checkAnyMotion() ) {
    	// Don't check the elapsed time.  If we got here it's time to turn off the lights as the schedule to get here
        // is overwritten when re-scheduled.  If there was a schedule to get here and then something happened
        // motionStopTime will be "Null"
        log.debug "Turning off lights"
		if (lights != null) {lights.off()}
        if (dimmers != null) {turnOffDimmers()}
		state.lastStatus = "off"
	}
}


//
// Turn on Dimmers
//
def turnOnDimmers() {

    if (settings.dimmers != null) {
        settings.dimmers?.each() {
            def name = it as String
            def fullName = "${name}_OnVal"
            def value = settings[fullName]
            log.debug("turnOnDimmers: value: $value")
            value = value.toInteger()
            if (value > 99) value = 99
            it.setLevel(value)
            log.debug("turnOnDimmers: Set $it to $value")
        }
    }
}


//
// Turn off Dimmers
//
def turnOffDimmers() {
    
    if (settings.dimmers != null) {
        settings.dimmers?.each() {
            def name = it as String
            def fullName = "${name}_OffVal"
            def value = settings[fullName]
            log.debug("turnOffDimmers: value: $value")
            value = value.toInteger()
            if (value > 99) value = 99
            it.setLevel(value)
            log.debug("turnOffDimmers: Set $it to $value")
        }
    }
}


//
// What to do on disable
//
def enableChangeActions(var) {
    log.debug "enableChangeActions: passed in $var"
    

    switch(var) {
      case "enableOff" :
        log.debug "enableChangeActions:doing enableOff"
        if (lights != null) {lights.off()}
        if (dimmers != null) {turnOffDimmers()}
		state.lastStatus = "off"
        break
      case "enableOffFull" :
        log.debug "enableChangeActions:doing enableOffFull"
        if (lights != null) {lights.off()}
        if (dimmers != null) {dimmers.setLevel(0)}
		state.lastStatus = "off"
        break
      case "enableOn" :
        log.debug "enableChangeActions:doing enableOn"
        if (lights != null) {lights.on()}
        if (dimmers != null) {turnOnDimmers()}
        state.lastStatus = "on"
        if (dimmers != null) {turnOnDimmers()}; // Double set to work around switch oddness
        break
      case "enableOnFull" :
        log.debug "enableChangeActions:doing enableOnFull"
        if (lights != null) {lights.on()}
        if (dimmers != null) {dimmers.setLevel(99)}
        state.lastStatus = "on"
        break
    }
}


//
// Get the Sun rise and set times
//
def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)    
    def current = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
    def result

	// If the riseTime is before now,
    // set to the next riseTime
	if(riseTime.before(current)) {
		riseTime = riseTime.next()
        log.debug "Setting to the next rise time"
	}

	// If the current stored rise time is not the next rise time
    // Update the schedule
	if (state.riseTime != riseTime.time) {
		unschedule("sunriseHandler")
		state.riseTime = riseTime.time

		log.info "scheduling sunrise handler for $riseTime"
		schedule(riseTime, sunriseHandler)
	}

	// If the setTime is before now,
    // set to the next setTime
	if(setTime.before(current)) {
		setTime = setTime.next()
	}
    
    // If the current stored set time is not the next set time
    // Update the schedule
	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
    
    log.debug "Current: $current"
    log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"

    // Code to set the enable state
    // Also done on the sunset and sunrise handlers
    // But done here incase the app is loaded after sunset
    // Sets a special state variable only used at init
	def t = now()
    log.debug "Time is $t"
    // If riseTime is greater than setTime we can just check if after setTime
    // the check is after set AND before rise
    if (state.riseTime > state.setTime) {
	    result = t < state.riseTime && t > state.setTime

		// Else (this means rise time is LESS then set time)
        // This can happen if the set time is updated before the current
        // day's rise time.
        // In this case we just check if we're before the rise time
    } else {
       	result = t < state.riseTime
    }
    state.astroEnabled = result
}

//
// Determine if the app is enabled and lights could be turned on
//
private enabled() {
	def result
    if (state.alwaysEnabled) {
    	log.debug "enabled(): Always Enabled"
    	result = true
	}
    // Else using sun rise/set or mode or light
	else {
        result = state.enabled
	}
    log.debug "Enabled: $result"
	return result
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}


