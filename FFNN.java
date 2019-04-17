package player;

public class FFNN {
	
	public int[] layers; // 中間層の大きさ。最初の値は入力層の、最後の値は出力層の大きさを入れる。
	public double[][] firstinput;
	public double[][] teacher;
	
	// ここから自動生成
	public int layernum;
	public int datanum;
	public double[][][] weights;
	public double[][] biases;
	public double[][][] outputs;
	public double[][][] outputsrelu;
	public double[] weighterrorsquare;
	public double[] biaserrorsquare;
	// ここまで自動生成
	
	public void Settings() {
		layernum = layers.length - 1;
		datanum = firstinput.length;
		weights = new double[layernum][][];
		biases = new double[layernum][];
		outputs = new double[layernum][][];
		outputsrelu = new double[layernum][][];
		outputsrelu[0] = firstinput;
		weighterrorsquare = new double[layernum];
		biaserrorsquare = new double[layernum];
		for (int i=0;i<layernum;i++) {
			weights[i] = new double[layers[i]][layers[i+1]];
			biases[i] = new double[layers[i+1]];
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
					output[i][j] = input[i][j];
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
		int yoko = outputs[layernum-1].length;
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
			weighterrorsquare[l] += tempnum;
			double eta = 0.01 / Math.sqrt(weighterrorsquare[l] + 0.00000001);
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
			biaserrorsquare[l] += tempnum;
			double eta = 0.01 / Math.sqrt(biaserrorsquare[l] + 0.00000001);
			for (int j=0;j<yoko;j++) {
				biases[l][j] -= eta * deltabiases[j];
			}
		}
	}
	
	public void Learn() {
		for (int i=0;i<layernum;i++) {
			outputs[i] = Affine(outputsrelu[i], weights[i], biases[i]);
			outputsrelu[i+1] = ReLU(outputs[i]);
		}
		double[][][] deltaoutputs = new double[layernum][][];
		double[][][] deltaweights = new double[layernum][][];
		deltaoutputs[layernum-1] = ErrorBackPropagation();
		deltaweights[layernum-1] = WeightBackPropagation(outputsrelu[layernum-1], deltaoutputs[layernum-1]);
		for (int i=layernum-2;i>=0;i--) {
			deltaoutputs[i] = ReLUBackPropagation(outputs[i], InputBackPropagation(weights[i], deltaoutputs[i+1]));
			deltaweights[i] = WeightBackPropagation(outputsrelu[i], deltaoutputs[i]);
		}
		AdaGradWeight(deltaweights);
		AdaGradBias(deltaoutputs);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
