package com.hethmon.bbq;

/**
 * Represents a 1 wire bus device
 * 
 * @author Paul Hethmon
 *
 */
public abstract class Sensor {

	private String serialNumber = null;
	private String devicePath = "/sys/bus/w1/devices/w1_bus_master1/";
	

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getDevicePath() {
		return devicePath;
	}

	public void setDevicePath(String devicePath) {
		if (devicePath != null) {
			// ensure device path always ends with the slash
			if (devicePath.endsWith("/") == false) {
				devicePath = devicePath + "/";
			}
		}
		this.devicePath = devicePath;
	}
	
	
}
