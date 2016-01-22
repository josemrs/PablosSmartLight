#!/usr/bin/env python2

import SocketServer
import threading
import Queue
import traceback
import time
import signal
import random

from neopixel import *

# LED strip configuration:
LED_COUNT      = 150     # Number of LED pixels.
LED_PIN        = 18      # GPIO pin connected to the pixels (must support PWM!).
LED_FREQ_HZ    = 800000  # LED signal frequency in hertz (usually 800khz)
LED_DMA        = 5       # DMA channel to use for generating signal (try 5)
LED_BRIGHTNESS = 100     # Set to 0 for darkest and 255 for brightest
LED_INVERT     = False   # True to invert the signal (when using NPN transistor level shift)

# Constants
HOST = "0.0.0.0"
PORT = 9999
INIT_SHOW_DELAY = 1
INIT_SHOW_BRIGHTNESS = 25
SXIT_SHOW_BRIGHTNESS = 3
CHASER_LENGTH = 20
CHASER_DELAY = 50
RANGE_BRIGHTNESS = 25
MAX_DELAY = 2000
MAX_BRIGHTNESS = 255
MAX_COLOR = 0xFFFFFF

# Communication queue between the CommandHandler and CommandProcessor threads
controlQueue = Queue.Queue()

# Flag to signal the threads to keep running or stop
keepRunning = True

# Create NeoPixel object with appropriate configuration.
ledStrip = Adafruit_NeoPixel(LED_COUNT, LED_PIN, LED_FREQ_HZ, LED_DMA, LED_INVERT, LED_BRIGHTNESS)

# Initializse the library (must be called once before other functions).
ledStrip.begin()

# Initilise  the random generator
random.seed()

# SIGTERM signal handler
def sigTermHandler(signum, frame):
	global keepRunning
	keepRunning = False

signal.signal(signal.SIGTERM, sigTermHandler)

class Colors(object):
	BLACK = Color(0, 0, 0);
	RED   = Color(255, 0, 0);
	GREEN = Color(0, 255, 0);
	BLUE  = Color(0, 0, 255);
	WHITE = Color(255, 255, 255);

class Position(object):
	COMMAND = 0
	RGB = 1
	BRIGHTNESS = 2
	DELAY = 3

class Command(object):
	SHUTDOWN = -1
	OFF = 0
	COLOR = 1
	RANDOM = 2
	CHASER = 3
	RANGE = 4

# Initialise global variables
command = Command.OFF
color = Color(0,0,0)
brightness = 0
delay = 10

# Thread to handler the commands comming from the network and queue them for processing.
class ControlSocketHandler(SocketServer.BaseRequestHandler):

	def handle(self):
		# self.request is the TCP socket connected to the client
		self.data = self.request.recv(1024).strip()
		
		print "Handling request received: " + self.data
		
		if len(self.data) > 25:
			print "Command too long"
			return
			
		received = self.data.split(',')
		print "Received from {} {} ({})".format(self.client_address[0], received, len(received))
		if len(received) != 4:
			print "Invalid command"
			return

		try:
			controlTuple = []
			controlTuple.append(int(received[0]))
			controlTuple.append(int(received[1], 16))
			controlTuple.append(int(received[2]))
			controlTuple.append(int(received[3]))
			
			if controlTuple[Position.COMMAND] < Command.SHUTDOWN or controlTuple[Position.COMMAND] > Command.RANGE:
				print "Unknown command"
			if controlTuple[Position.RGB] > MAX_COLOR:
				print "Color out of range"
			if controlTuple[Position.BRIGHTNESS] > MAX_BRIGHTNESS:
				print "Brightness out of range"
			if controlTuple[Position.DELAY] > MAX_DELAY:
				print "Delay out of range"
				return
		except ValueError:
			raise
			print "Invalid value in command"
			return
		except:
			print "Unknown error"
			return
			
		print "Control tuple {}".format(controlTuple)
		controlQueue.put(controlTuple)

# Server thread listening to the network port
class ControlCommandListener(threading.Thread):
	def __init__(self, host, port):
		threading.Thread.__init__(self)
		self.server = SocketServer.TCPServer((host, port), ControlSocketHandler)

	def run(self):
		print "Starting ControlCommandListener"
		self.server.serve_forever()

	def stop(self):
		print "Stopping ControlCommandListener"
		self.server.shutdown()

# Dequeue commands and process them setting up the global variables
# This is done here instead of main to allow faster LED refresh
class ControlCommandProcessor(threading.Thread):
	def __init__(self):
		threading.Thread.__init__(self)
	
	def run(self):
		global keepRunning
		global command
		global color
		global brightness
		global delay
		
		print "Starting ControlCommandProcessor"
		while keepRunning:
			try:
				controlTuple = controlQueue.get(timeout=3)
				
				print "Processing command received: {}".format(controlTuple)
								
				if controlTuple[0] == Command.SHUTDOWN:
					print "Shutting down"
					keepRunning = False
				else:
					if controlTuple[Position.COMMAND] == Command.OFF:
						print "OFF"
						brightness = 0
						print "Off"
						command = Command.OFF
					
					elif controlTuple[Position.COMMAND] == Command.COLOR:
						color = controlTuple[Position.RGB]
						brightness = controlTuple[Position.BRIGHTNESS]
						delay = controlTuple[Position.DELAY]
						print "RGB Color 0x%02x brightness %d delay %d" % (color, brightness, delay)
						command = Command.COLOR
					
					elif controlTuple[Position.COMMAND] == Command.RANDOM:
						brightness = controlTuple[Position.BRIGHTNESS]
						delay = controlTuple[Position.DELAY]
						print "Random, brightness %d delay %d" % (brightness, delay)
						command = Command.RANDOM
					
					elif controlTuple[Position.COMMAND] == Command.CHASER:
						#color = controlTuple[Position.RGB]
						brightness = controlTuple[Position.BRIGHTNESS]
						delay = controlTuple[Position.DELAY]
						print "Chaser Color 0x%02x brightness %d delay %d" % (color, brightness, delay)
						command = Command.CHASER
					
					elif controlTuple[Position.COMMAND] == Command.RANGE:
						color = controlTuple[Position.RGB]
						min = color & 0xFF
						value = (color >> 8) & 0xFF
						max = (color >> 16) & 0xFF
						#brightness = controlTuple[Position.BRIGHTNESS]
						#delay = controlTuple[Position.DELAY]
						print "Range %d [%d, %d] brightness %d delay %d" % (value, min, max, brightness, delay)
						command = Command.RANGE

			except Queue.Empty:
				pass
			except:
				print "Exception handling command"
				raise
				keepRunning = False

	def stop(self):
		print "Stopping ControlCommandProcessor"

# Shows the passed in color in the LEDs
def showColor(color, brightness, wait_ms=25):
	ledStrip.setBrightness(brightness)
	for i in range(ledStrip.numPixels()):
		ledStrip.setPixelColor(i, color)
		if wait_ms > 0:
			ledStrip.show()
			time.sleep(wait_ms/1000.0)
	
	ledStrip.show()

# Turns the LEDs on in random colors
def showRandomColors(brightness):
	ledStrip.setBrightness(brightness)
	
	for i in range(ledStrip.numPixels()):
		color = random.randrange(0, 0xFFFFFF)
		ledStrip.setPixelColor(i, color)
	ledStrip.show()

# Enables CHASER_LENGTH LEDs and move them along the strip
def showChaser(brightness, chaserPosition):
	ledStrip.setBrightness(brightness)
	color = random.randrange(0, 0xFFFFFF)

	if chaserPosition < CHASER_LENGTH:
		ledStrip.setPixelColor(chaserPosition, color)

	elif chaserPosition >= CHASER_LENGTH and chaserPosition <= ledStrip.numPixels():
		ledStrip.setPixelColor(chaserPosition - CHASER_LENGTH, 0)
		ledStrip.setPixelColor(chaserPosition, color)

	elif chaserPosition > ledStrip.numPixels():
		ledStrip.setPixelColor(chaserPosition - CHASER_LENGTH, 0)

	ledStrip.show()

# Shows the position of a value in a range.
def showRange(min, value, max):
	fixedDelay = 10
	ledStrip.setBrightness(RANGE_BRIGHTNESS)
	
	if max < min:
		inverted = True
		lowColor = 0xFF0000
		hiColor = 0x0000FF
		min, max = max, min
	else:
		inverted = False
		lowColor = 0x0000FF
		hiColor = 0xFF0000
		
	normalColor = 0x00FF00
	
	OldRange = max - min
	NewRange = ledStrip.numPixels()
	NewValue = (((value - min) * NewRange) / OldRange)
	
	lowLevel = ledStrip.numPixels() / 3
	hiLevel = (ledStrip.numPixels() / 3) * 2
	
	for position in range(0, NewValue):
		if position < lowLevel:
			ledStrip.setPixelColor(position, lowColor)
		elif position > hiLevel:
			ledStrip.setPixelColor(position, hiColor)
		else:
			ledStrip.setPixelColor(position, normalColor)
		
		ledStrip.show()
		time.sleep(fixedDelay/1000.0)
	
# Shows red, green, blue and white, then turn off.
def initShow():
	showColor(Colors.RED, INIT_SHOW_BRIGHTNESS, 0)
	time.sleep(INIT_SHOW_DELAY)
	showColor(Colors.GREEN, INIT_SHOW_BRIGHTNESS, 0)
	time.sleep(INIT_SHOW_DELAY)
	showColor(Colors.BLUE, INIT_SHOW_BRIGHTNESS, 0)
	time.sleep(INIT_SHOW_DELAY)
	showColor(Colors.WHITE, INIT_SHOW_BRIGHTNESS, 0)
	time.sleep(INIT_SHOW_DELAY)
	showColor(Colors.BLACK, 0, 0)

# Shows low brightness red
def exitShow():
	showColor(Colors.RED, EXIT_SHOW_BRIGHTNESS, 0)

if __name__ == "__main__":

	chaserPosition = 0
	commandProcessor = None

	try:
		control = ControlCommandListener(HOST, PORT)
		control.setDaemon(True)
		control.start()

		initShow()
		
		commandProcessor = ControlCommandProcessor()
		commandProcessor.setDaemon(True)
		commandProcessor.start()

		while keepRunning:
			oldCommand = command

			if command == Command.OFF:
				showColor(0x0, 0, delay)
				time.sleep(1)
			if command == Command.COLOR:
				showColor(color, brightness, delay)
				time.sleep(1)
			elif command == Command.RANDOM:
				showRandomColors(brightness)
				time.sleep(delay/1000.0)	
			elif command == Command.CHASER:
				chaserPosition += 1
				chaserPosition %= ledStrip.numPixels() + CHASER_LENGTH
				showChaser(brightness, chaserPosition)
				time.sleep(CHASER_DELAY/1000.0)
			elif command == Command.RANGE:
				min = color & 0xFF
				value = (color >> 8) & 0xFF
				max = (color >> 16) & 0xFF
				showRange(min, value, max)
				time.sleep(2)
				command = Command.OFF

			if oldCommand != command:
				showColor(Colors.BLACK, 0, 0)
			
    	except KeyboardInterrupt:
			keepRunning = False

	control.stop()
	control.join()

	if commandProcessor is not None:
		commandProcessor.stop()
		commandProcessor.join()
	
	exitShow()
