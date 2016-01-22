#!/usr/bin/env python2

import Adafruit_DHT
import Adafruit_BMP085
import threading
import time
import signal
import Queue
import SocketServer


# Constants
HOST = "0.0.0.0"
PORT = 9998
DHT_SENSOR = Adafruit_DHT.DHT22
DHT_GPIO_PIN = 4
POLLING_RATE = 5
BMP_ADDRESS = 0x77
FACTOR = 10

keepRunning = True

# SIGTERM signal handler
def sigTermHandler(signum, frame):
	global keepRunning
	keepRunning = False

signal.signal(signal.SIGTERM, sigTermHandler)

# Server thread listening to the network port
class NetworkServer(threading.Thread):
	def __init__(self):
		threading.Thread.__init__(self)
		self.server = SocketServer.TCPServer((HOST, PORT), SendInformation)

	def run(self):
		print "Starting NetworkServer"
		self.server.serve_forever()

	def stop(self):
		print "Stopping NetworkServer"
		self.server.shutdown()
		
bmp = Adafruit_BMP085.BMP085(BMP_ADDRESS)
informationQueue = Queue.Queue(1)

class SendInformation(SocketServer.BaseRequestHandler):

	def handle(self):
	
		info = [False]	# No valid information
	
		try:
			info = informationQueue.get(True, POLLING_RATE*2)
		except Queue.Empty:
			print "Exception: No information available"
		except:
			print "Unknown Exception"	
			raise
		
		try:
			response = ""
			if info:
				if info[0]:
					response += "1 "	# DHT data valid
					response += "{0:d} {1:d} ".format(info[1], info[2])
					if info[3] == 1:
						response += "1 "	# BMP data valid
						response += "{0:d} {1:d} ".format(info[4], info[5])
					else:
						response += "0 "	# BMP data not valid
				else:
					response += "0 "	# DHT data not valid
					if info[1]:
						response += "1 " 	# BMP data valid
						response += "{0:d} {1:d}".format(info[2], info[3])
					else:
						response += "0"	# BMP data not valid
				
				response += "\n"
				self.request.sendall(response)
		except Queue.Empty:
			print "Exception: No information available"
		except:
			print "Unknown Exception"	
			raise		
	
class SensorsPolling(threading.Thread):
	def __init__(self):
		threading.Thread.__init__(self)
	
	def run(self):
		global keepRunning
		global informationQueue
		
		print "Starting SensorsPolling"
		while keepRunning:
			newInfo = [False] # No valid info yet
			
			try:
				
				# Try to grab a sensor reading.  Use the read_retry method which will retry up
				# to 15 times to get a sensor reading (waiting 2 seconds between each retry).
				humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_GPIO_PIN)

				if humidity is not None and temperature is not None:
					#print 'Temp={0:0.1f}*C  Humidity={1:0.1f}%'.format(temperature, humidity)
					temperature = int(temperature * FACTOR)
					humidity = int(humidity * FACTOR)
					newInfo = [True, temperature, humidity]
				else:
					print 'Failed to get reading from DHT sensor'
				
				pressure = bmp.readPressure()
				bmpTemperature = bmp.readTemperature()
				
				if pressure is not None and bmpTemperature is not None:
					#print 'Press={0:0.1f}*C  Alt={1:0.1f} Temp={1:0.1f}*C'.format(pressure, altitude, bmpTemperature)
					newInfo.append(True)
					newInfo.append(int(pressure/100.0 * FACTOR))
					newInfo.append(int(bmpTemperature * FACTOR))
				else:
					print 'Failed to get reading from BMP sensor'
					newInfo.append(False)
				
			except:
				print "Exception polling sensors"
				raise
				keepRunning = False

			with informationQueue.mutex: informationQueue.queue.clear()	# Remove previous information
			informationQueue.put(newInfo)
			time.sleep(POLLING_RATE)

	def stop(self):
		global keepRunning
		
		print "Stopping SensorsPolling"
		keepRunning = False


if __name__ == "__main__":

	chaserPosition = 0

	sensorsPolling = None
	networkServer = None
	
	try:		
		sensorsPolling = SensorsPolling()
		sensorsPolling.setDaemon(True)
		sensorsPolling.start()
		
		networkServer = NetworkServer()
		networkServer.setDaemon(True)
		networkServer.start()

		while threading.active_count() > 0:
			time.sleep(2)
		
	except KeyboardInterrupt:
		print "Ctrl-C pressed"
	except:
		print "Exception in main"
		
	if sensorsPolling is not None:
		sensorsPolling.stop()
		
	if networkServer is not None:
		networkServer.stop()

