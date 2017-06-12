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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
    
preferences {
	page(name: "firstPage")
    page(name: "secondPage")
    page(name: "thirdPage")
}

// First page
def firstPage() {
	dynamicPage(name: "firstPage", title: "Select lights and triggers", nextPage: "thirdPage", install: false, uninstall: true) {
        section("Control these lights...") {
            input "lights", "capability.switch", multiple: true
        }
        section("Turning on if there's movement..."){
            input "motionSensor", "capability.motionSensor", title: "Motion?", multiple: true, required: false
        }
        section("Or when a door opens...") {
            input "doors", "capability.contactSensor", title: "Door?", multiple: true, required: false
        }
        section("Or when someone arrives") {
            input "people", "capability.presenceSensor", multiple: true, required:false
        }
        section("And then off when it's light or there's been no movement, or all doors are closed for..."){
            input "delayMinutes", "number", title: "Minutes?"
        }
        section("Persistent Mode?") {
        	input "perst", "bool", defaultValue: "true", required: false
        }
        section("Pick when this app will be enabled") {
            input("enableType", "enum", options: [
                "alwaysEnabledChoice":"Always Enabled",
                "lightSensorChoice":"Light Sensor",
                "sunChoice": "Sun Rise/Set"], submitOnChange: true)
        }
        
        // Options are based on enable type
        if (enableType == "alwaysEnabledChoice") {
            section("No additional setup for Always Enabled")

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

// Page to setup based on Enabled Type
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

def thirdPage() {
	dynamicPage(name: "thirdPage", title: "Name app and configure modes", install: true, uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def installed() {
	initialize()
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
    
	if (enableType == "alwaysEnabledChoice") {
    	state.alwaysEnabled = true
        def lightSensor = null
    } else if (enableType == "lightSensorChoice") {
    	state.alwaysEnabled = false
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	} else {
    	state.alwaysEnabled = false
        def lightSensor = null
        state.riseTime = 0
        state.setTime = 0
		subscribe(location, "position", locationPositionChange)
		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		astroCheck()
        // schedule an astro check every 1h to work around SmartThings missing scheduled events issues
        runEvery1Hour(astroCheck)
	}
    state.lastStatus = "unknown"
    log.debug "Initialize: Always Enabled: $state.alwaysEnabled"
    log.debug "Initialize: Light Sensor: $lightSensor"
    log.debug "Initialize: Persistent mode: $perst"
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
				lights.on()
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
			lights.on()
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
    // Turn off lights when it gets bright
	if (lastStatus != "off" && evt.integerValue > 50) {
		lights.off()
		state.lastStatus = "off"
	}
    // Check if it just got dark enough to turn on the lights
	else if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
		log.debug "turning on lights since it's now dark and there is motion or something opened"
		lights.on()
		state.lastStatus = "on"
	}
	state.motionStopTime = null
}

//
// Presence Handler
//
def presenceHandler(evt) {
	log.debug "$evt.name: $evt.value"
    // Don't do anything is the off time is 0 or we'd
    // just turn the light on then off
	if (evt.value == "present" && delayMinutes != 0) {
		if (enabled()) {
			log.debug "turning on lights due to presence"
			lights.on()
			state.lastStatus = "on"
		}
		state.motionStopTime = null
        // Now see if an off event should be scheduled
        scheduleCheckTurnOff()
	}
}


//
// Sunset Handler
//  Check for any open doors to turn lights on if they were opened before
//
def sunsetHandler() {
	log.debug "Executing sunset handler"
    // Check if there is a door open to turn on the lights
	if (checkDoorsOpen()) {
		log.debug "turning on lights since it's now sunset and there is something opened"
		lights.on()
		state.lastStatus = "on"
	}
	state.motionStopTime = null
}
    	
//
// Sunrise Handler
//  Turn off lights at sunrise
//
def sunriseHandler() {
	log.debug "Executing sunrise handler"
    // Turn off lights when it gets bright
	if (lastStatus != "off") {
		log.debug "turning off lights since it's now sunrise"
		lights.off()
		state.lastStatus = "off"
	}
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
	log.trace "In checkTurnOff, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    // Check only if:
    	// - there is a stopTimeSet (this is set to Null when we turn on),
        // - All doors are closed and there is no motion
	if (state.motionStopTime && !checkDoorsOpen() && !checkAnyMotion() ) {
    	// Don't check the elapsed time.  If we got here it's time to turn off the lights as the schedule to get here
        // is overwritten when re-scheduled.  If there was a schedule to get here and then something happened
        // motionStopTime will be "Null"
        log.debug "Turning off lights"
		lights.off()
		state.lastStatus = "off"
	}
}


//
// Get the Sun rise and set times
//
def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)    
    def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset

	// If the riseTime is before now,
    // set to the next riseTime
	if(riseTime.before(now)) {
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
	if(setTime.before(now)) {
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
    
    log.debug "Now: $now"
    log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
    
}

//
// Determine if the app is enabled and lights could be turned on
//
private enabled() {
	def result
    if (state.alwaysEnabled) {
    	log.debug "enabled(): Always Enabled"
    	result = true
	} else if (lightSensor) {
		result = lightSensor.currentIlluminance?.toInteger() < 30
	}
    // Else using set rise/set
	else {
		def t = now()
        log.debug "Time is $t"
        log.debug "Rise is $state.riseTime"
        log.debug "Set is $state.setTime"
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
	}
    log.debug "Enabled: $result"
	result
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

