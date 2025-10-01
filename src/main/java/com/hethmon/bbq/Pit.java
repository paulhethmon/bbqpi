package com.hethmon.bbq;

import java.time.Instant;
import java.time.LocalDateTime;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Monitor the BBQ Pit temperature and run the fan to adjust it as needed.
 *
 */
@Slf4j
@Component
public class Pit {
	
	private static final String version = "1.0.0 -- 02 September 2025";
	
	private static Logger cookingLog = LoggerFactory.getLogger("COOKING");

    @Autowired
    private SendMessage sendMessage;

    @Getter
    @Setter
	protected double targetTemp = 225.0;
    @Getter
    @Setter
    private String fromEmailAddress;
    @Getter
    @Setter
    private String toEmailAddress;

    protected double lowTemp = targetTemp - 10.0;
	protected double highTemp = targetTemp + 10.0;
	protected double wayTooHighTemp = targetTemp + 50.0;
	protected double wayTooLowTemp = targetTemp - 30.0;

    @Getter
    @Setter
	private int sleepSeconds = 60;
    @Getter
    @Setter
	private boolean tellPitMaster = false;
	
	protected Integer[] hourList;
    private Context pi4j;
	
	/**
	 * Default constructor.
	 */
	public Pit() {
        pi4j = Pi4J.newAutoContext();
    	hourList = new Integer[24];
    	for (int i = 0; i < 24; i++) {
    		hourList[i] = 0;
    	}
	}
	
	/**
	 * Set the target temperature.
	 * 
	 * @param targetTemp
	 */
	public Pit(double targetTemp) {
        pi4j = Pi4J.newAutoContext();

        this.targetTemp = targetTemp;
		lowTemp = targetTemp - 10.0;
		highTemp = targetTemp + 10.0;
		wayTooLowTemp = targetTemp - 75.0;
		wayTooHighTemp = targetTemp + 75.0;
		
    	hourList = new Integer[24];
    	for (int i = 0; i < 24; i++) {
    		hourList[i] = 0;
    	}
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
    		
    		do {
    			temp = takeTemp();
    			cookingLog.info("Pit temperature = [{}]", temp);
                log.info("Pit temperature = [{}]", temp);
    			if (tellPitMaster == true) {
    				tellThePitMaster(temp);
    			}
    			
    			if (temp < lowTemp) {
    				// need to raise the temp
    				lowCount++;
    				highCount = 0; // reset high
    				log.info("Low temp count = [{}]", lowCount);
    			}
    			
    			if (temp > highTemp) {
    				// temp is too high
    				highCount++;
    				lowCount = 0; // reset low
    				log.info("High temp count = [{}]", highCount);
    			}
    			
    			// if lowCount is greater than 2, then do something to fix it
    			if (lowCount > 2) {
    				if (pin == null) {
    					pin = getPin();
    				}
    				log.info("Low temp count = [{}]. Making the fire hotter.", lowCount);
    				makeHotter(pin); // stoke the fire
    				lowCount = 0;
    			}
    			// if the temp is below our minimum, do something
    			if (temp < wayTooLowTemp) {
    				log.info("Current temperature is [{}]", temp);
    				log.info("Way Too Low temperature is set to [{}]", wayTooLowTemp);
    				if (pin == null) {
    					pin = getPin();
    				}
    				log.info("Temperature is way below. Making the fire hotter.", lowCount);
    				makeHotter(pin);
    				getHelp(temp);
    			}
    			
    			// now let's check the hot side of things
    			if (highCount > 2) {
       				if (pin == null) {
    					pin = getPin();
    				}
    				log.info("High temp count = [{}]. Making the fire colder.", lowCount);
    				makeColder(pin);
    				//getHelp(temp);
    				highCount = 0;
    			}
    			if (temp > wayTooHighTemp) {
    				log.info("Current temperature is [{}]", temp);
    				log.info("Way Too High temperature is set to [{}]", wayTooHighTemp);
       				if (pin == null) {
    					pin = getPin();
    				}
    				log.info("Temperature is way high. Making the fire colder.", lowCount);
    				makeColder(pin);
    				getHelp(temp);
    			}
    			
    			sleep1();
    			
    		} while (true) ;
    		
    	} catch (Exception e) {
    		log.error("Exception in main program.", e);
    		return false;
    	}
    }
    
    /**
     * Turn on the fan and take a temperature reading every minute until we reach temperature.
     * Toggling the gpio to a low state turns on the fan.
     * 
     * @param pin
     */
    protected void makeHotter(DigitalOutput pin) {
    	
    	System.out.println("Making the temperature hotter.");
		log.info("Turning ON the fan.");
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
    	
    	System.out.println("Making the temperature colder.");
		log.info("Turning OFF the fan.");
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
    			cookingLog.error("Unable to send email.", e);
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
    
    protected void tellThePitMaster(double temp) {
    	try {
	    	LocalDateTime now = LocalDateTime.now();
	    	int hour = now.getHour();
	    	if (hourList[hour] != 0) return;
	    	hourList[hour] = 1;
	    	
	    	String msg = "It's " + hour + " o'clock. The BBQ Pit is at " + temp + " degrees\r\n";
	    	msg += "\r\n\r\n";
	    	msg += "That sure is some pig!\r\n";
	    	msg += "\r\n\r\n";
	    	msg += "This message thoughtfully brought to you by the Barley Pi BBQ Pit Monitor.\r\n";
	    	msg += "\r\n\r\n";

            sendMessage.sendEmail(fromEmailAddress, toEmailAddress,
                    "Pig Roast Update", msg);

	    	//GMail.sendGmail("Pig Roast Update", msg, "tvh-all@knoxhomebrewer.com");
	    	System.out.println("The temperature is [" + temp + "]");
    	
    	} catch (Exception e) {
    		
    	}
    }

    /**
     * Create the controller for the fan
     * @return
     */
    protected DigitalOutput getPin() {
        log.info("Creating gpio PIN on pin 5 ...");
        // build the config we need
        DigitalOutputConfig config = DigitalOutput.newConfigBuilder(pi4j)
                .id("Fan")
                .name("Fan Controller")
                .address(5)
                .initial(DigitalState.HIGH)
                .build();
        // create the actual entity
        DigitalOutput digitalOutput = pi4j.create(config);

        // sleep for a moment for hardware to be set up fully
        try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
        log.info("Built PIN 5 as [{}]", digitalOutput);
        return digitalOutput;
    }
    
    protected void sleep1() {
        try {
			Thread.sleep(1000 * sleepSeconds * 1);
		} catch (InterruptedException e) {
		}
    }
    
    protected void sleep5() {
        try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
		}
    }


}
