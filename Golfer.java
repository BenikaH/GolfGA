public class Golfer{
	private String name;
	private double pts;
	private int salary;
	private int golferNumber;
	
	public Golfer(){
		name = "";
		pts = 0;
		salary = 0;
		golferNumber = 0;
	}
	
	public Golfer(String myName, int mySalary, double myPts, int myGolferNumber){
		name = myName;
		salary = mySalary;
		pts = myPts;
		golferNumber = myGolferNumber;
	}
	
	public String getName(){
		return name;
	}
	
	public int getSalary(){
		return salary;
	}
	
	public double getPts(){
		return pts;
	}
	
	public int getGolferNumber(){
		return golferNumber;
	}
	
	public void setName(String myName){
		name = myName;
	}
	
	public void setSalary(int mySalary){
		salary = mySalary;
	}
	
	public void setPts(double myPts){
		pts = myPts;
	}
	
	public void setGolferNumber(int myGolferNumber){
		golferNumber = myGolferNumber;
	}
	
	@Override
	public String toString(){
		return name + "\t" + salary + "\t" + pts + "\t" + golferNumber;
	}
}