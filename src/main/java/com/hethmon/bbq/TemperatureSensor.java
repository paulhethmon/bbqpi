package com.hethmon.bbq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Represents a 1 wire bus temperature sensor
 * 
 * @author Paul Hethmon
 *
 */
@Slf4j
public class TemperatureSensor extends Sensor {

	private String fileName = null;
	
	private TemperatureSensor() {
		// noop
	}
	
	public TemperatureSensor(String serialNumber) {
		setSerialNumber(serialNumber);
		fileName = getDevicePath() + getSerialNumber() + "/w1_slave";
	}
	
	private String getTemperature() {
		String temp = null;
		
		ArrayList<String> data = getFileData(fileName);
		// must be two lines
		if (data == null) {
			log.error("No data found for device at [{}]", fileName);
			return null;
		}
		if (data.get(0).endsWith("YES") == false) {
			return "";
		}
		temp = data.get(1).substring( data.get(1).indexOf("t=") + 2 );
		
		
		return temp;
	}
	
	public double getCelsius() {
		double temp = 0.0;
		try {
			String sTemp = getTemperature();
			
			temp = Double.parseDouble(sTemp);
			temp = temp / 1000;
			
			return temp;
		} catch (Exception e) {
			return 0.0;
		}
	}
	
	public double getFahrenheit() {
		double temp = getCelsius();
		
		temp = 9.0 * temp/5.0 + 32.0;
		
		return temp;
	}
	
	private ArrayList<String> getFileData(String filename) {
		BufferedReader brIn;
		FileReader fr;
		String sLine;
		ArrayList<String> dataLines = new ArrayList<String>();

		try {
			fr = new FileReader(filename);
			brIn = new BufferedReader(fr);

			while ((sLine = brIn.readLine()) != null) {
				dataLines.add(sLine.trim());
			}
			brIn.close();
			fr.close();
		} catch (Exception e) {
			log.error("Exception reading device data at [{}]", filename);
			log.error("Exception reading device data.", e);
			return null;
		}

		return dataLines;
	}

}
