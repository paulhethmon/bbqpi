package com.hethmon.bbq;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;


/**
 * Monitor the BBQ Pit temperature and run the fan to adjust it as needed.
 *
 */
@Slf4j
@Component
public class Gpio {

	private static final String version = "1.0.0 -- 02 September 2025";

    private Context pi4j;

	/**
	 * Default constructor.
	 */
	public Gpio() {
        pi4j = Pi4J.newAutoContext();
	}

	/**
	 * Start the temperature monitor and adjust the fan as needed to maintain the temperature.
	 * 
	 * @return false if there is an exception
	 */
    public boolean cook() {
        DigitalOutput pin =  null;
    	double temp = 0.0;
    	int highCount = 0;
    	int lowCount = 0;

        pin = getPin();
    	
    	System.out.println(version);

        try {

            if (pin == null) {
                pin = getPin();
            }
            log.info("Turning on the relay", lowCount);
            makeHotter(pin); // stoke the fire
            sleep5();

            if (pin == null) {
                pin = getPin();
            }
            log.info("Turning off the relay", lowCount);
            makeColder(pin);
            sleep1();


        } catch (Exception e) {
            log.error("Exception in main program.", e);
            return false;
        }

        return true;
    }
    
    /**
     * Turn on the fan and take a temperature reading every minute until we reach temperature.
     * Toggling the gpio to a low state turns on the fan.
     * 
     * @param pin
     */
    protected void makeHotter(DigitalOutput pin) {

        log.info("Turning ON the fan.");
    	pin.low();
    	System.out.println("Turning fan on. (PIN low)");
    }

    /**
     * Turn on the fan and take a temperature reading every minute until we reach temperature.
     * Toggling the gpio to a high state turns off the fan.
     * 
     * @param pin
     */
    protected void makeColder(DigitalOutput pin) {

        log.info("Turning OFF the fan.");
    	pin.high();
    	System.out.println("Turning fan off. (PIN high)");
    }

    
    private long timeBetween = 1000 * 60 * 5;
    private Instant lastTimeSent = Instant.now();
    protected void getHelp(double temp) {
    	Instant now = Instant.now();
    	
    	if (now.toEpochMilli() -  lastTimeSent.toEpochMilli() > timeBetween) {
    		lastTimeSent = Instant.now();
    		String msg = "The temperature is out of range. HELP! [" + temp + "]";
    		try {
//    		GMail.sendGmail("Alert", msg);
    		} catch (Exception e) {
    			log.error("Unable to send email.", e);
    		}
    	}
    	System.out.println("The temperature is out of range. HELP! [" + temp + "]");
    }

    protected double takeTemp() {
    	// one wire bus is located on GPIO-04 pin
    	TemperatureSensor ts = new TemperatureSensor("3b-0000001833fa");
    	double farenheit = ts.getFahrenheit();
    	System.out.println("Temp is " + farenheit);
        log.info("Temp is [{}]", farenheit);
    	return farenheit;
    }

    /**
     * Create the controller for the fan
     * @return
     */
    protected DigitalOutput getPin() {
        // Or you can use a configuration builder
        var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("my-dout")
                .name("My LED").bcm(5)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.HIGH);
        var led = pi4j.create(ledConfig);

//
//        log.info("Creating gpio PIN on pin 5 ...");
//        // build the config we need
//        DigitalOutputConfig config = DigitalOutput.newConfigBuilder(pi4j)
//                .id("Fan")
//                .name("Fan Controller")
//                .address(5)
//                .initial(DigitalState.HIGH)
//                .build();
//        // create the actual entity
//        DigitalOutput digitalOutput = pi4j.create(config);
//
//        // sleep for a moment for hardware to be set up fully
//        try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//		}
        log.info("Built PIN 5 as [{}]", led);
        return led;
    }
    
    protected void sleep1() {
        try {
			Thread.sleep(1000 * 1);
		} catch (InterruptedException e) {
		}
    }
    
    protected void sleep5() {
        try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
		}
    }

    public void test() {
        // Initialize Pi4J with an auto context
// An auto context includes AUTO-DETECT BINDINGS enabled
// which will load all detected Pi4J extension libraries
// (Platforms and Providers) in the class path
        var pi4j = Pi4J.newAutoContext();

// create a digital output instance using the default digital output provider
        var output = pi4j.dout().create(5);
        output.config().shutdownState(DigitalState.HIGH);

// setup a digital output listener to listen for any state changes on the digital output
        output.addListener(System.out::println);

// lets invoke some changes on the digital output
        output.state(DigitalState.HIGH)
                .state(DigitalState.LOW)
                .state(DigitalState.HIGH)
                .state(DigitalState.LOW);

// lets toggle the digital output state a few times
        output.toggle()
                .toggle()
                .toggle();

// another friendly method of setting output state
        output.high()
                .low();

// lets read the digital output state
        System.out.print("CURRENT DIGITAL OUTPUT [" + output + "] STATE IS [");
        System.out.println(output.state() + "]");

// pulse to HIGH state for 3 seconds
        System.out.println("PULSING OUTPUT STATE TO HIGH FOR 3 SECONDS");
        output.pulse(3, TimeUnit.SECONDS, DigitalState.HIGH);
        System.out.println("PULSING OUTPUT STATE COMPLETE");

// shutdown Pi4J
        pi4j.shutdown();

    }
}
