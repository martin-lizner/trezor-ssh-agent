@echo off
git pull
mvn clean compile
cd examples
mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.usb.UsbMonitoringExample"
cd ..
