package player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class FFNN {
	
	public int[] layers; // 中間層の大きさ。最初の値は入力層の、最後の値は出力層の大きさを入れる。
	public double[][] firstinput;
	public double[][] teacher;
	public String savedirectory;
	public boolean usermsprop;
	public double rmspropalpha = 0.99;
	public double alpha = 0.001;
	
	// ここから自動生成
	public int layernum;
	public int datanum;
	public double[][][] weights;
	public double[][] biases;
	public double[][][] outputs;
	public double[][][] outputsrelu;
	public double[] weighterrorsquare;
	public double[] biaserrorsquare;
	public double[] weightaverages;
	public double[] weightstandards;
	public double[] biasaverages;
	public double[] biasstandards;
	public boolean readdata = false;
	// ここまで自動生成
	
	public void Settings() {
		layernum = layers.length - 1;
		datanum = firstinput.length;
		outputs = new double[layernum][][];
		outputsrelu = new double[layernum][][];
		outputsrelu[0] = firstinput;
		weightaverages = new double[layernum];
		weightstandards = new double[layernum];
		biasaverages = new double[layernum];
		biasstandards = new double[layernum];
		Random rd = new Random(System.currentTimeMillis());
		if (!readdata) {
			weights = new double[layernum][][];
			biases = new double[layernum][];
			weighterrorsquare = new double[layernum];
			biaserrorsquare = new double[layernum];
			for (int i=0;i<layernum;i++) {
				weights[i] = new double[layers[i]][layers[i+1]];
				biases[i] = new double[layers[i+1]];
				for (int j=0;j<layers[i];j++) {
					for (int k=0;k<layers[i+1];k++) {
						weights[i][j][k] = rd.nextDouble() / 100;
					}
				}
				outputs[i] = new double[datanum][layers[i+1]];
			}
		}
		else {
			for (int i=0;i<layernum;i++) {
				outputs[i] = new double[datanum][layers[i+1]];
			}
		}
		for (int i=1;i<layernum;i++) {
			outputsrelu[i] = new double[datanum][layers[i]];
		}
		savedirectory = "C:\\Users\\sakur\\Documents\\java\\scoredqn";
	}
	
	public void Normalization(double[][] input) {
		double tempsum = 0;
		double tempsum2 = 0;
		int inputsize = input.length * input[0].length;
		for (int i=0;i<input.length;i++) {
			for (int j=0;j<input[i].length;j++) {
				tempsum += input[i][j];
				tempsum2 += input[i][j] * input[i][j];
			}
		}
		double average = tempsum / inputsize;
		double standard = Math.sqrt(average * average - tempsum2 / inputsize);
	}
	
	public void ChangeInput(double[][] input) {
		firstinput = input;
		datanum = firstinput.length;
		outputsrelu[0] = firstinput;
		for (int i=0;i<layernum;i++) {
			outputs[i] = new double[datanum][layers[i+1]];
		}
		for (int i=1;i<layernum;i++) {
			outputsrelu[i] = new double[datanum][layers[i]];
		}
	}
	
	public double[][] Affine(double[][] input, double[][] weight, double[] bias) {
		int inputnum = input[0].length;
		int outputnum = bias.length;
		double[][] output = new double[datanum][outputnum];
		for (int i=0;i<datanum;i++) {
			for (int j=0;j<outputnum;j++) {
				double tempnum = bias[j];
				for (int k=0;k<inputnum;k++) {
					tempnum += input[i][k] * weight[k][j];
				}
				output[i][j] = tempnum;
			}
		}
		return output;
	}
	
	public double[][] ReLU(double[][] input){
		int tate = input.length;
		int yoko = input[0].length;
		double[][] output = new double[tate][yoko];
		for (int i=0;i<tate;i++) {
			for (int j=0;j<yoko;j++) {
				if (input[i][j] > 0) {
					if (input[i][j] > 1000) {
						output[i][j] = 1000;
					}
					else {
						output[i][j] = input[i][j];
					}
				}
				else {
					output[i][j] = 0;
				}
			}
		}
		return output;
	}
	
	public double[][] InputBackPropagation(double[][] weight, double[][] deltaoutput){
		int tate = deltaoutput.length;
		int tako = weight.length;
		int yoko = weight[0].length;
		double[][] deltainput = new double[tate][tako];
		for (int i=0;i<tate;i++) {
			for (int k=0;k<tako;k++) {
				double tempnum = 0;
				for (int j=0;j<yoko;j++) {
					tempnum += deltaoutput[i][j] * weight[k][j];
				}
				deltainput[i][k] = tempnum;
			}
		}
		return deltainput;
	}
	
	public double[][] WeightBackPropagation(double[][] input, double[][] deltaoutput){
		int tate = input.length;
		int tako = input[0].length;
		int yoko = deltaoutput[0].length;
		double[][] deltaweight = new double[tako][yoko];
		for (int k=0;k<tako;k++) {
			for (int j=0;j<yoko;j++) {
				double tempnum = 0;
				for (int i=0;i<tate;i++) {
					tempnum += deltaoutput[i][j] * input[i][k];
				}
				deltaweight[k][j] = tempnum;
			}
		}
		return deltaweight;
	}
	
	public double[][] ReLUBackPropagation(double[][] input, double[][] deltaoutput){
		int tate = input.length;
		int yoko = input[0].length;
		double[][] deltainput = new double[tate][yoko];
		for (int i=0;i<tate;i++) {
			for (int j=0;j<yoko;j++) {
				if (input[i][j] > 0) {
					deltainput[i][j] = deltaoutput[i][j];
				}
				else {
					deltainput[i][j] = 0;
				}
			}
		}
		return deltainput;
	}
	
	public double[][] ErrorBackPropagation(){
		int tate = outputs[layernum-1].length;
		int yoko = outputs[layernum-1][0].length;
		double[][] deltaoutput = new double[tate][yoko];
		for (int i=0;i<tate;i++) {
			for (int j=0;j<yoko;j++) {
				deltaoutput[i][j] = outputs[layernum-1][i][j] - teacher[i][j];
			}
		}
		return deltaoutput;
	}
	
	public void AdaGradWeight(double[][][] deltaweights) {
		for (int l=0;l<layernum;l++) {
			int tate = deltaweights[l].length;
			int yoko = deltaweights[l][0].length;
			double tempnum = 0;
			for (int i=0;i<tate;i++) {
				for (int j=0;j<yoko;j++) {
					tempnum += deltaweights[l][i][j] * deltaweights[l][i][j];
				}
			}
			if (usermsprop) {
				weighterrorsquare[l] = weighterrorsquare[l] * rmspropalpha + tempnum * (1 - rmspropalpha);
			}
			else {
				weighterrorsquare[l] += tempnum;
			}
			double eta = alpha / Math.sqrt(weighterrorsquare[l] + 0.00000001);
			for (int i=0;i<tate;i++) {
				for (int j=0;j<yoko;j++) {
					weights[l][i][j] -= eta * deltaweights[l][i][j];
				}
			}
		}
	}
	
	public void AdaGradBias(double[][][] deltaoutputs) {
		for (int l=0;l<layernum;l++) {
			int tate = deltaoutputs[l].length;
			int yoko = deltaoutputs[l][0].length;
			double[] deltabiases = new double[yoko];
			double tempnum = 0;
			for (int j=0;j<yoko;j++) {
				for (int i=0;i<tate;i++){
					deltabiases[j] += deltaoutputs[l][i][j];
				}
				tempnum += deltabiases[j] * deltabiases[j];
			}
			if (usermsprop) {
				biaserrorsquare[l] = biaserrorsquare[l] * rmspropalpha + tempnum * (1 - rmspropalpha);
			}
			else {
				biaserrorsquare[l] += tempnum;
			}
			double eta = alpha / Math.sqrt(biaserrorsquare[l] + 0.00000001);
			for (int j=0;j<yoko;j++) {
				biases[l][j] -= eta * deltabiases[j];
			}
		}
	}
	
	public void ForwardPropagation() {
		for (int i=0;i<layernum-1;i++) {
			outputs[i] = Affine(outputsrelu[i], weights[i], biases[i]);
			outputsrelu[i+1] = ReLU(outputs[i]);
		}
		outputs[layernum-1] = Affine(outputsrelu[layernum-1], weights[layernum-1], biases[layernum-1]);
	}
	
	public void Learn() {
		for (int i=0;i<layernum-1;i++) {
			outputs[i] = Affine(outputsrelu[i], weights[i], biases[i]);
			outputsrelu[i+1] = ReLU(outputs[i]);
		}
		outputs[layernum-1] = Affine(outputsrelu[layernum-1], weights[layernum-1], biases[layernum-1]);
		double[][][] deltaoutputs = new double[layernum][][];
		double[][][] deltaweights = new double[layernum][][];
		deltaoutputs[layernum-1] = ErrorBackPropagation();
		deltaweights[layernum-1] = WeightBackPropagation(outputsrelu[layernum-1], deltaoutputs[layernum-1]);
		for (int i=layernum-2;i>=0;i--) {
			deltaoutputs[i] = ReLUBackPropagation(outputs[i], InputBackPropagation(weights[i+1], deltaoutputs[i+1]));
			deltaweights[i] = WeightBackPropagation(outputsrelu[i], deltaoutputs[i]);
		}
		AdaGradWeight(deltaweights);
		AdaGradBias(deltaoutputs);
	}
	
	public void Save() {
		Date date = new Date();
		String newdirplace = savedirectory + "\\" + date.toString().replace(' ', '_').replace(':', '_');
		File newdir = new File(newdirplace + "\\");
		newdir.mkdir();
		try {
			for (int i=0;i<weights.length;i++) {
				FileWriter fw;
				fw = new FileWriter(newdirplace + "\\weight_" + i + ".csv");
				fw.write(MatrixToCSV(weights[i]));
				fw.close();
			}
			for (int i=0;i<biases.length;i++) {
				FileWriter fw = new FileWriter(newdirplace + "\\bias_" + i + ".csv");
				fw.write(MatrixToCSV(biases[i]));
				fw.close();
			}

			FileWriter fw0 = new FileWriter(newdirplace + "\\weighterrorsquare.csv");
			fw0.write(MatrixToCSV(weighterrorsquare));
			fw0.close();
			FileWriter fw1 = new FileWriter(newdirplace + "\\biaserrorsquare.csv");
			fw1.write(MatrixToCSV(biaserrorsquare));
			fw1.close();
		} 
		catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}	
	}
	
	
	public String MatrixToCSV(double[][] matrix) {
		return Arrays.deepToString(matrix).replace("],", "\r\n").replace("[", "").replace("]", "");
	}
	
	public String MatrixToCSV(double[] matrix) {
		String output = Double.toString(matrix[0]);
		for (int i=1;i<matrix.length;i++) {
			output += "," + Double.toString(matrix[i]);
		}
		return output;
	}
	
	public double[][] CSVToMatrix(String filename){
		try {
			List<String> stringlines = Files.readAllLines(Paths.get(filename));
			double[][] output = new double[stringlines.size()][];
			for (int i=0;i<stringlines.size();i++) {
				String[] tempstr = stringlines.get(i).split(",");
				output[i] = new double[tempstr.length];
				for (int j=0;j<tempstr.length;j++) {
					output[i][j] = Double.parseDouble(tempstr[j]);
				}
			}
			return output;
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return null;
		}
	}
	
	public double[] CSVToVector(String filename){
		try {
			String[] tempstr = Files.readAllLines(Paths.get(filename)).get(0).split(",");
			double[] output = new double[tempstr.length];
			for (int i=0;i<tempstr.length;i++) {
				output[i] = Double.parseDouble(tempstr[i]);
			}
			return output;
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			return null;
		}
	}
	
	public void ReadLearned(String directory) {
		File readdir = new File(directory);
		File[] files = readdir.listFiles();
		int weightnum = 0;
		for (int i=0;i<files.length;i++) {
			if (files[i].getName().contains("weight_")) {
				weightnum++;
			}
		}
		weights = new double[weightnum][][];
		biases = new double[weightnum][];
		for (int i=0;i<files.length;i++) {
			String[] namew = files[i].getName().split("weight_");
			String[] nameb = files[i].getName().split("bias_");
			if (files[i].getName().contains("error")) {
				if (files[i].getName().contains("weight")) {
					weighterrorsquare = CSVToVector(directory + "\\" + files[i].getName());
				}
				if (files[i].getName().contains("bias")) {
					biaserrorsquare = CSVToVector(directory + "\\" + files[i].getName());
				}
			}
			else if (namew.length > 1) {
				weights[Integer.parseInt(namew[1].split(".csv")[0])] = CSVToMatrix(directory + "\\" + files[i].getName());
			}
			else if (nameb.length > 1) {
				biases[Integer.parseInt(nameb[1].split(".csv")[0])] = CSVToVector(directory + "\\" + files[i].getName());
			}
		}
		readdata = true;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
