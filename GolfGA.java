import java.util.Random;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.net.URLDecoder;
import java.io.InputStreamReader;
import java.io.InputStream;

public class GolfGA{
	private static final int teamsPerGen = 200;
	private static final int playersPerTeam = 6;
	private static final int bitsPerPlayer = 8;
	private static final int numGens = 1000;
	
	private static final double crossProb = .3;
	private static final double mutProb = .05;
	
	//make class variable so that can seed rng once for all methods
	private Random random;
	
	private int numGolfNumbers;	//this is size of golfNumber HashMap. Also equal to number of possible encodings
	private HashMap<String,Integer> golferNumber;	//key is a player encoding, value is corresponding golfer number
	private ArrayList<Golfer> golfers;
	private int numGolfers;		//size of golfers ArrayList, i.e. total number of golfers to pick from
	private String[] curGen;
	private String[] nextGen;
	private double[] fitness;
	private int[] parents;
	
	public static void main(String args[]){
		System.out.println("Program is running...");
		
		long startTime;
		long endTime;
		long totalTime;
			
		startTime = System.currentTimeMillis();
		
		GolfGA ga = new GolfGA();
		ga.run();

		endTime = System.currentTimeMillis();
		totalTime = (endTime - startTime) / 1000;
		System.out.println("Total run time = " + totalTime + " seconds");
	}
	
	public GolfGA(){
		numGolfNumbers = (int)Math.pow(2,bitsPerPlayer);
		golferNumber = new HashMap<String,Integer>();
		golfers = new ArrayList<Golfer>();
		curGen = new String[teamsPerGen];
		nextGen = new String[teamsPerGen];
		fitness = new double[teamsPerGen];
		parents = new int[teamsPerGen];
		random = new Random(1);
	}
	
	public void run(){
		String basePath = getBasePath();
		String inputFile = basePath + "projections.csv";
		String initialFile = basePath + "initial.csv";
		String finalFile = basePath + "final.csv";
		
		getGolfers(inputFile);	//currently this is reading in csv from RG with player names and projected points totals
		//printGolfers();
		setGolferNumber();
		//printGolferNumber();
		initialize();
		printTeamsToFile(initialFile);
		System.out.println("Average fit to start = " + getAvgFit());
		for(int i = 0; i < numGens; i++){
			eval();	
			selection();
			crossover();
			mutation();
			resetVars();
		}
		printTeamsToFile(finalFile);
		System.out.println("Average fit to finish = " + getAvgFit());
	}
	
	//get the path to the class file or jar file that is being run
	public String getBasePath(){
		int pos;
		String basePath = "";
		String path;
		String decodedPath = "";
		File file;
		
		try{
			path = GolfGA.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			decodedPath = URLDecoder.decode(path, "UTF-8");
		}catch(Exception e){
			System.out.println(e);
		}
	
		basePath = decodedPath;
		if(basePath.contains("/C:/")){
		    pos = basePath.indexOf("/C:/");
		    basePath = basePath.substring(pos+1);
	    }
		
		file = new File(basePath);
		//if program is being run from a jar, then file will be location of jar file. Otherwise, it should be the correct basePath
		if(file.isFile()){
			basePath = 	file.getParentFile().getPath();
			basePath = basePath.replace("\\","/");
			basePath = basePath + "/";
		}
		
		return basePath;
	}
	
	//reads in golfers names and projected point totals from file (default is from RG LineupBuilder)
	public void getGolfers(String inputFile){
		BufferedReader br;
		String curLine;
		String values[] = new String[2];
		Golfer curGolfer;
		String name;
		double pts;
		int curGolferNumber = 0;
		
		try{
			br = new BufferedReader(new FileReader(inputFile));
			//used this instead for jar file
			//br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("projections.csv")));
			//remove header
			curLine = br.readLine();
			if(curLine != null){
				while((curLine = br.readLine()) != null){
					curLine = curLine.replace("\"","");
					values = curLine.split(",");
					name = values[0];
					pts = Double.parseDouble(values[1]);
					curGolfer = new Golfer(name,0,pts,curGolferNumber);
					golfers.add(curGolfer);
					
					curGolferNumber++;	
				}
			}
		}catch(Exception e){
			System.out.println(e);
		}
		
		numGolfers = curGolferNumber;
	}
	
	//the encoding should be able to represent all of the golfers in the uploaded file
	//for example, there may be 150 golfers. Then at least 8 bits should be used for encoding each golfer, so that there are 256 possible encodings
	//this method maps each encoding to a golfer. For example, if there are 150 golfers and 8 bit encoding, an encoding of 0-149 maps to golfers 1-150
	//remaining encodings of 150-255 map to golfers 1-106
	public void setGolferNumber(){
		int quotient = numGolfNumbers / numGolfers;
		int remainder = numGolfNumbers % numGolfers;
		int index = 0;
		int index2 = 0;
		String bitArray;
		
		for(int i = 0; i < quotient; i++){
			for(int j = 0; j < numGolfers; j++){
				index = i * numGolfers + j;
				bitArray = convertNumberToBits(index);
				golferNumber.put(bitArray,j);
			}
		}
		index++;
		for(int i = index; i < numGolfNumbers; i++){
			bitArray = convertNumberToBits(i);
			golferNumber.put(bitArray,i - index);
		}
	}
	
	//create first generation with number of individuals equal to teamsPerGen. Number of bits per individual is equal to playersPerTeam * bitsPerPlayer
	//each bit is randomly selected to be either "0" or "1"
	public void initialize(){
		double randNum;
		String nextBit;
		String curTeam;
		
		for(int i = 0; i < teamsPerGen; i++){
			curGen[i] = "";
			nextGen[i] = "";
			curTeam = "";
			for(int j = 0; j < playersPerTeam * bitsPerPlayer; j++){
				randNum = random.nextDouble();
				if(randNum < .5){
					nextBit = "0";
				}else{
					nextBit = "1";
				}
				curTeam = curTeam + nextBit;
			}
			curGen[i] = curTeam;
		}
	}
	
	//evaluate the fitness of each individual in current generation.
	//currently fitness is sum of projected point totals for all golfers on a team
	//point totals come from uploaded file
	//default file is projections downloaded from RotoGrinders.com LineupBuilder
	public void eval(){
		String curTeam;
		String curPlayer;
		int curNumber;
		int curGolferNumber;
		Golfer curGolfer;
		double curGolferPts;
		double teamPts;

		for(int i = 0; i < teamsPerGen; i++){
			teamPts = 0;
			curTeam = curGen[i];
			for(int j = 0; j < playersPerTeam; j++){
				curPlayer = curTeam.substring(j*bitsPerPlayer,(j+1)*bitsPerPlayer);
				curGolferNumber = golferNumber.get(curPlayer);
				curGolfer = golfers.get(curGolferNumber);
				curGolferPts = curGolfer.getPts();
				teamPts = teamPts + curGolferPts;
			}
			fitness[i] = teamPts;
		}
	}
	
	//"roulette-wheel" selection. Likliehood an individual in current generation being selected to be a parent of the next generation is equal to that
	//individuals fitness divided by the sum of the populations fitness
	public void selection(){
		double totFit = 0;
		double randNum = 0;
		int curTeam;
		double cumFit = 0;
		
		for(int i = 0; i < teamsPerGen; i++){
			totFit = totFit + fitness[i];
		}
		
		for(int i = 0; i < teamsPerGen; i++){
			randNum = random.nextDouble();
			cumFit = 0;
			curTeam = 0;
			while(cumFit / totFit < randNum && curTeam < teamsPerGen){
				cumFit = cumFit + fitness[curTeam];
				curTeam++;
			}
			if(curTeam == teamsPerGen && cumFit / totFit < randNum){
				System.out.println("Possible error. Very unlikely that curTeam should equal teamsPerGen and cumFit / totFit < randNum here");
				System.out.println("randNum="+randNum);
				System.out.println("cumFit / totFit ="+ cumFit / totFit );
			}
			curTeam--;
			parents[i] = curTeam;
		}
	}
	
	//successive pairs of parents are used to produce two children for next generation. One-point crossover is used
	public void crossover(){
		double randNum;
		String child1 = "";
		String child2 = "";
		int half = playersPerTeam * bitsPerPlayer / 2;
		int full = playersPerTeam * bitsPerPlayer;
		
		for(int i = 0; i < teamsPerGen / 2; i++){
			randNum = random.nextDouble();
			child1 = "";
			child2 = "";
			
			//grab half the bits from one parent and half the bits from the other parent
			if(randNum < crossProb){
				child1 = curGen[parents[2*i]].substring(0,half) + curGen[parents[2*i+1]].substring(half,full);
				child2 = curGen[parents[2*i+1]].substring(0,half) + curGen[parents[2*i]].substring(half,full);
			//no crossover
			}else{
				child1 = curGen[parents[2*i]];
				child2 = curGen[parents[2*i+1]];
			}
			nextGen[2*i] = child1;
			nextGen[2*i+1] = child2;
		}
		
		//if there is an odd number of teams per generation, just copy over last team from previous generation to next generation
		if(teamsPerGen % 2 == 1){
			nextGen[teamsPerGen - 1] = curGen[parents[teamsPerGen - 1]];
		}
	}
	
	public void mutation(){
		double randNum;
		String curChild = "";
		String mutChild = "";
		String curBit;

		for(int i = 0; i < teamsPerGen; i++){
			curChild = nextGen[i];
			mutChild = "";
			
			for(int j = 0; j < playersPerTeam * bitsPerPlayer; j++){
				randNum = random.nextDouble();
				
				curBit = curChild.substring(j,j+1);
				if(randNum < mutProb){
					if(curBit.equals("0")){
						curBit = "1";
					}else{
						curBit = "0";
					}
				}
				mutChild = mutChild + curBit;
			}
			
			nextGen[i] = mutChild;
		}
	}
	
	//call this method after finishing computing the current generation to reset variables for the next generation
	public void resetVars(){
		curGen = nextGen;
		nextGen = new String[teamsPerGen];
		fitness = new double[teamsPerGen];
		parents = new int[teamsPerGen];
	}
	
	//giving an encoding of bits of size bitsPerPlayer, returns the decimal equivalent
	public int convertBitsToNumber(String bitArray){
		int numBits = bitArray.length();
		String curBit = "";
		int finalNum = 0;
		int curNum = 0;
		
		for(int i = 0; i < numBits; i++){
			curBit = bitArray.substring(numBits-i-1,numBits-i);
			curNum = 0;
			if(curBit.equals("1")){
				curNum = (int)Math.pow(2,i);
			}
			finalNum = finalNum + curNum;
		}
		
		return finalNum;
	}
	
	//given a number (0 to numGolfNumbers - 1), returns a string encoding of size bitsPerPlayer
	public String convertNumberToBits(int number){
		String bitArray = "";
		int numBits;
		
		while(number > 0){
			if(number % 2 == 0){
				bitArray = "0" + bitArray;
			}else{
				bitArray = "1" + bitArray;
			}
			number = number / 2;
		}
		
		//add leading 0's so that bitArray is of proper size
		numBits = bitArray.length();
		for(int i = 0; i < bitsPerPlayer - numBits; i++){
			bitArray = "0" + bitArray;	
		}
		
		return bitArray;
	}
	
	public void printGolfers(){
		for(int i = 0; i < numGolfers; i++){
			System.out.println(golfers.get(i));
		}
	}
	
	public void printGolferNumber(){
		Iterator itr;
		String curPlayer;
		Integer curGolferNumber;
		
		itr = golferNumber.keySet().iterator();
		while(itr.hasNext()){
			curPlayer = (String) itr.next();
			curGolferNumber = golferNumber.get(curPlayer);
			System.out.println("curPlayer = " + curPlayer + "\tcurGolferNumber = " + curGolferNumber);
		}
	}
	
	public void printCurGen(){
		for(int i = 0; i < teamsPerGen; i++){
			System.out.println(curGen[i]);
		}
	}
	
	public void printNextGen(){
		for(int i = 0; i < teamsPerGen; i++){
			System.out.println(nextGen[i]);
		}
	}
	
	public void printFitness(){
		for(int i = 0; i < teamsPerGen; i++){
			System.out.println(fitness[i]);
		}	
	}
	
	public void printParents(){
		for(int i = 0; i < teamsPerGen; i++){
			System.out.println(parents[i]);
		}	
	}
	
	//prints curGen to specified file
	public void printTeamsToFile(String filePath){
		BufferedWriter bw;
		String curTeam;
		String curPlayer;
		int curGolferNumber;
		Golfer curGolfer;
		String curGolferName;
		double curGolferPts;
		double teamPts;
		
		try{
			bw = new BufferedWriter(new FileWriter(filePath));
			
			for(int i = 0; i < teamsPerGen; i++){
				curTeam = curGen[i];
				teamPts = 0;
				for(int j = 0; j < playersPerTeam; j++){
					curPlayer = curTeam.substring(j*bitsPerPlayer,(j+1)*bitsPerPlayer);
					curGolferNumber = golferNumber.get(curPlayer);
					curGolfer = golfers.get(curGolferNumber);
					curGolferName = curGolfer.getName();
					curGolferPts = curGolfer.getPts();
					teamPts = teamPts + curGolferPts;
					bw.write(curGolferName + ",");
				}
				bw.write(String.valueOf(teamPts));
				bw.newLine();
			}
				
			bw.close();
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	//return the average fitness over all teams in a generation. Used to determine how well the GA is performing
	public double getAvgFit(){
		double avgFit = 0;
		String curTeam;
		String curPlayer;
		int curGolferNumber;
		Golfer curGolfer;
		String curGolferName;
		double curGolferPts;
		double teamPts;
		
		for(int i = 0; i < teamsPerGen; i++){
			curTeam = curGen[i];
			teamPts = 0;
			for(int j = 0; j < playersPerTeam; j++){
				curPlayer = curTeam.substring(j*bitsPerPlayer,(j+1)*bitsPerPlayer);
				curGolferNumber = golferNumber.get(curPlayer);
				curGolfer = golfers.get(curGolferNumber);
				curGolferName = curGolfer.getName();
				curGolferPts = curGolfer.getPts();
				teamPts = teamPts + curGolferPts;
			}
			avgFit = avgFit + teamPts;
		}
		avgFit = avgFit / (double)teamsPerGen;
		
		return avgFit;
	}
	
	public void testBitToNumConv(){
		String bitArray;
		
		bitArray = "0";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "10";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "11";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "100";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "101";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "110";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "111";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1000";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1001";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1010";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1011";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1100";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1101";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1110";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		bitArray = "1111";
		System.out.println(bitArray + ": " + convertBitsToNumber(bitArray));
		
	}
	
	
	public void testNumToBitConv(){
		int number;
		
		number = 0;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 1;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 2;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 3;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 4;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 5;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 6;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 7;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 8;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 9;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 10;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 11;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 12;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 13;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 14;
		System.out.println(number + ": " + convertNumberToBits(number));
		number = 15;
		System.out.println(number + ": " + convertNumberToBits(number));
		
	}
}