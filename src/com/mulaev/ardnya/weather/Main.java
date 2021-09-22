package com.mulaev.ardnya.weather;

import java.io.PrintStream;

public class Main {
	
	public static void main(String[] args) throws Exception {
		Logic execObj = new Logic("ru","Saint Petersburg", "bb395a72a7d81ef513946f27bccebba1", 5);
		
		switch(args.length) {
			case 3 : {
				execObj.setCountry(args[0]);
				execObj.setCity(args[1]);
				execObj.setDays(Integer.valueOf(args[2]));
			} break;
		
			case 4 : {
				execObj.setCountry(args[0]);
				execObj.setCity(args[1]);
				execObj.setAppId(args[2]);
				execObj.setDays(Integer.valueOf(args[3]));
			} break;
		} 
		
		try {
			execObj.doAction();
		} catch (Exception e) {
			System.out.println("Проверьте корректность вводимых данных или повторите попытку позднее!");
			
			try (PrintStream error = new PrintStream("log.txt")) {
				e.printStackTrace(error);
			}
		}
	}
}
