// ISensorService.aidl
package com.mmx.service;

// Declare any non-default types here with import statements

interface ISensorService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
	void sendRequiredDataToService(in Bundle data);
}
