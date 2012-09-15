import java.util.*;

/*
 * Calculate sunset and sunrise on a given day on a given location
 * 
 * GPL, MIT License
 * 2012, Žan Kafol, Ana Grum
 */

public class Analemma {
	
	/*
	 * Test - print out sunset for all days in this year
	 */
	public static void main(String[] args) {
		Calendar calendar = Calendar.getInstance();
		
		// our chosen latitude coordinate
		double latitude = 45.772792;
		
		// for each month
		for (int m = 0; m < 12; m++) {
			calendar.set(Calendar.MONTH, m);
			
			// for each day in that month
			for (int d = 1; d <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH); d++) {
				
				System.out.println(sunset(latitude, calendar.get(Calendar.YEAR), m, d));
			}
		}
	}
	
	/*
	 * Returns a Date object representing the time and date of the sunset for
	 * the given parameters. The formula takes into account the daylight saving
	 * time offset.
	 * 
	 * @param	double	latitude coordinate for calculating the sunset
	 * @param	int		the year for calculating the sunset
	 * @param	int		the month (0..January, 1..February, ...) for calculating the sunset
	 * @param	int		the day of month for calculating the sunset
	 * @return			the	time and date of the sunset
	 * @see				dayLength
	 */
	public static Date sunset(double latitude, int year, int month, int day) {
		double dayLength = dayLength(latitude, year, month, day);
		double hour = 12 + dayLength / 2 + daylightSaving(year, month, day);
		
		return getDate(year, month, day, hour);
	}
	
	/*
	 * Returns a Date object representing the time and date of the sunrise for
	 * the given parameters. The formula takes into account the daylight saving
	 * time offset.
	 * 
	 * @param	double	latitude coordinate for calculating the sunrise
	 * @param	int		the year for calculating the sunrise
	 * @param	int		the month (0..January, 1..February, ...) for calculating the sunrise
	 * @param	int		the day of month for calculating the sunrise
	 * @return			the time and date of the sunrise
	 * @see 			dayLength
	 */
	public static Date sunrise(double latitude, int year, int month, int day) {
		double dayLength = dayLength(latitude, year, month, day);
		double hour = 12 - dayLength / 2 + daylightSaving(year, month, day);
		
		return getDate(year, month, day, hour);
	}
	
	/*
	 * Returns the length of a day for the given parameters in hours. The
	 * formula takes into account leap years.
	 * 
	 * @param	double	latitude coordinate for calculating the length of the day
	 * @param	int 	the year for calculating the length of the day
	 * @param	int 	the month (0..January, 1..February, ...) for calculating the length of the day
	 * @param	int 	the day of month for calculating the length of the day
	 * @return			the day length in fractional hours
	 * @see				GregorianCalendar
	 */
	public static double dayLength(double latitude, int year, int month, int day) {
		// Define the cardinal dates
		GregorianCalendar 
			today			= new GregorianCalendar(year,		month,				day), 
			prevWinter		= new GregorianCalendar(year - 1,	Calendar.DECEMBER,	21), 
			nextSpting		= new GregorianCalendar(year + 1,	Calendar.MARCH,		21), 
			springStart		= new GregorianCalendar(year,		Calendar.MARCH,		21), 
			summerStart		= new GregorianCalendar(year,		Calendar.JUNE,		21), 
			autumnStart		= new GregorianCalendar(year,		Calendar.SEPTEMBER,	23), 
			winterStart		= new GregorianCalendar(year,		Calendar.DECEMBER,	21);
		
		int		season; 		// number of days in the season of the date
		int		cardinal;		// number of days passed since the start of the season
		
		boolean	isWinter = false;
		boolean	isAutumn = false;
		
		if (today.after(prevWinter) && today.before(springStart)) {
			season = daysBetween(prevWinter, springStart);
			cardinal = daysBetween(today, prevWinter);
			isWinter = true;
		} else if (today.equals(springStart) || today.after(springStart) && today.before(summerStart)) {
			season = daysBetween(springStart, summerStart);
			cardinal = daysBetween(today, springStart);
		} else if (today.equals(summerStart) || today.after(summerStart) && today.before(autumnStart)) {
			season = daysBetween(summerStart, autumnStart);
			cardinal = daysBetween(today, summerStart);
		} else if (today.equals(autumnStart) || today.after(autumnStart) && today.before(winterStart)) {
			season = daysBetween(autumnStart, winterStart);
			cardinal = daysBetween(today, autumnStart);
			isAutumn = true;
		} else {
			season = daysBetween(winterStart, nextSpting);
			cardinal = daysBetween(today, winterStart);
			isWinter = true;
		}
		
		// Calculate the Sun's declination
		double declination = (cardinal * 23.5 / season);
		
		// During a solstice, the maximum axial tilt to the Sun is 23°26'15"
		// During an equinox, the axial tilt to the Sun is 0°
		if (today.after(summerStart) && today.before(autumnStart) || today.before(springStart) || today.after(winterStart)) {
			declination = 23.5 - declination;
		}
		
		// Summer and winter solstice
		if (declination == 0 && !(today.equals(springStart) || today.equals(autumnStart))) {
			declination = 23.5;
		}
		
		// Use a negative declination between the summer's and next winter solstice
		if (isWinter || isAutumn) {
			declination *= -1;
		}
		
		// Calculate the day lenght from latitude and declination
		double cos_t = -Math.tan(Math.toRadians(latitude)) * Math.tan(Math.toRadians(declination));
		double t = Math.toDegrees(Math.acos(cos_t));
		double dayLength = 2 * t * 24 / 360;
		
		return dayLength;
	}

	/*
	 * Returns the number of days passed between two dates
	 * 
	 * @param	GregorianCalendar	first date
	 * @param	GregorianCalendar 	second date
	 * @return						days passed
	 * @see							GregorianCalendar
	 */
	public static int daysBetween(GregorianCalendar d1, GregorianCalendar d2) {
		double millis = Math.abs(d1.getTimeInMillis() - d2.getTimeInMillis());
		return (int) (millis / (1000 * 3600 * 24));
	}
	
	/*
	 * Returns 1 if the date is in daylight time or 0 if not
	 * 
	 * @param	int		year of the date
	 * @param	int		month of the date
	 * @param	int		day of the month of the date
	 * @return			1 for true, 0 for false
	 * @see				TimeZone
	 */
	public static int daylightSaving(int year, int month, int day) {
		return TimeZone.getDefault().inDaylightTime(getDate(year, month, day, 12, 0)) ? 1 : 0;
	}

	/*
	 * Returns the Date object. This method is used as a replacement for the
	 * deprecated constructor Date(year, month, day)
	 * 
	 * @param	int		year of the date
	 * @param	int		month of the date
	 * @param	int		day of the month of the date
	 * @return			the Date object for the given parameters
	 * @see				Date
	 */
	public static Date getDate(int year, int month, int day) {
		return new Date(new GregorianCalendar(year, month, day).getTimeInMillis());
	}
	
	/*
	 * Returns the Date object. This method is used as a replacement for the
	 * deprecated constructor Date(year, month, day, hour, minute)
	 * 
	 * @param	int		year of the date
	 * @param	int		month of the date
	 * @param	int		day of the month of the date
	 * @param 	int		hour of day
	 * @param 	int		minute of hour
	 * @return			the Date object for the given parameters
	 * @see				Date
	 */
	public static Date getDate(int year, int month, int day, int hour, int min) {
		return new Date(new GregorianCalendar(year, month, day, hour, min).getTimeInMillis());
	}
	
	/*
	 * Returns the Date object. This method is used as a replacement for the
	 * deprecated constructor Date(year, month, day, hour, minute, second)
	 * 
	 * @param	int		year of the date
	 * @param	int		month of the date
	 * @param	int		day of the month of the date
	 * @param 	int		hour of day as double
	 * @return			the Date object for the given parameters
	 * @see				Date
	 */
	public static Date getDate(int year, int month, int day, double hour) {
		double min = (hour - Math.floor(hour)) * 60;
		double sec = (min - Math.floor(min)) * 60;
		return new Date(new GregorianCalendar(year, month, day, (int) hour, (int) min, (int) sec).getTimeInMillis());
	}
}
