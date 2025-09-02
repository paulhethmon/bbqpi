package com.hethmon.bbq;

import com.pi4j.Pi4J;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@Slf4j
@SpringBootApplication
public class BbqApplication implements ApplicationRunner {

    @Autowired
    private Pit pit;

	public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BbqApplication.class);
        // tell spring to not start the netty web server
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
	}

    private boolean doPit = false;
    private boolean doMail = false;
    private int targetTemp = 225;
    private String fromEmailAddress = null;
    private String toEmailAddress = null;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("BBQ Pi Starting ...");

        if (args.getOptionNames().size() < 1) {
            printHelp();
            return;
        }

        args.getOptionNames().forEach(optionName -> {
            if (optionName.equals("help") == true) {
                printHelp();
            } else if (optionName.equals("pit") == true) {
                doPit = true;
            } else if (optionName.equals("mail") == true) {
                doMail = true;
            } else if (optionName.equals("temp") == true) {
                List<String> stringList = args.getOptionValues(optionName);
                String tmp = stringList.get(0);
                try {
                    targetTemp = Integer.parseInt(tmp);
                } catch (NumberFormatException e) {
                    log.error("Unable to parse temp of [{}]. Defaulting to [{}].", tmp, targetTemp);
                }
                log.info("Received target temperature of [{}].", targetTemp);
            } else if (optionName.equals("fromEmail") == true) {
                List<String> stringList = args.getOptionValues(optionName);
                fromEmailAddress = stringList.get(0);
                log.info("Received from email address of [{}].", fromEmailAddress);
            } else if (optionName.equals("toEmail") == true) {
                List<String> stringList = args.getOptionValues(optionName);
                toEmailAddress = stringList.get(0);
                log.info("Received to email address of [{}].", toEmailAddress);
            } else {
                log.error("Unknown option [{}] = {}", optionName, args.getOptionValues(optionName));
            }
        });

        if (doPit == true) {
            pit.setTargetTemp(targetTemp);
            pit.setTellPitMaster(doMail);
            pit.setFromEmailAddress(fromEmailAddress);
            pit.setToEmailAddress(toEmailAddress);
            pit.cook();
        }



    }

    private void printHelp() {
        log.info("Help Stuff Here");
    }

}
