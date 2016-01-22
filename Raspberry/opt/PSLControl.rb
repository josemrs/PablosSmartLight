#!/usr/bin/env ruby

require 'sinatra'
require 'socket'
require 'json'

set :bind, '0.0.0.0'

$ControlIP = 'localhost'
$LEDControlPort = 9999
$SensorsPort = 9998

get '/' do
	"Pablo's WonderLight Control REST API"
end

######### LEDs #########

get '/leds' do
	begin
		s = TCPSocket.new $ControlIP, $LEDControlPort
		s.close
	rescue
		status 404
	end
end

put '/leds/:cmd' do
	begin
		s = TCPSocket.new $ControlIP, $LEDControlPort
		
		begin
			request.body.rewind
			payload = JSON.parse request.body.read
			puts payload
			
			command = params['cmd']
			if command == "off"
				command = 0
			elsif command == "color"
				command = 1
			elsif command == "random"
				command = 2
			elsif command == "chaser"
				command = 3
			elsif command == "range"
				command = 4
			end

			rgb = payload['RGB']
			brightness = payload['brightness']
			delay = payload['delay']
			s.puts "#{command},#{rgb},#{brightness},#{delay}\n"
		rescue
			puts "Exception parsing command"
		end

		s.close
	rescue
		puts "Exception connection to LEDControl"
		status 404
	end
end

######### SENSORS #########

get '/sensors' do

	dhtValid = false
	bmpValid = false
	temperature = 0
	humidity = 0
	pressure = 0
	bmptemperature = 0

	begin
		s = TCPSocket.new $ControlIP, $SensorsPort
		
		begin
			received = s.gets
			info = received.split(' ')

			if info[0] == "1"
				dhtValid = true
				temperature = info[1]
				humidity = info[2]
				if info[3] == "1"	# Valid DHT and BMP data
					bmpValid = true
					pressure = info[4]
					bmpTemperature = info[5]
				end
			elsif info[1] == "1"	# DHT data not valid but valid BMP data
				bmpValid = true
				pressure = info[2]
				bmpTemperature = info[3]
			end
		rescue
			status 404
			puts "Exception parsing information"
		end

		s.close
	rescue
		puts "Exception connection to Sensors service"
		status 404
	end
	
	content_type :json
	
	{ :dhtvalid => dhtValid, 
	  :temperature => "#{temperature}", :humidity => "#{humidity}", 
	  :bmpvalid => bmpValid, 
	  :pressure => "#{pressure}", :bmptemperature => "#{bmpTemperature}"
	}.to_json

end

