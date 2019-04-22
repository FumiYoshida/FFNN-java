package player;

import java.util.Arrays;

public class Evaluation {
	/*
 　このぷよぷよでは自分と相手が同時に手を決めるため、
	零和だが囚人のジレンマに似た状況が発生する。
	そこで、自分の行動m通りと相手の行動n通りに基づく、m*nの評価値の行列を作成し、
	その行列をもとに最適な選択の確率分布を求める。
	具体的には、まず自分がランダムに手を選択するとしたとき、
	相手はどの選択をどの確率で取ると相手の勝率が高くなるかを考え、
	その確率分布をもとに、自分がどの選択をどの確率で取ると自分の勝率が高くなるかを考える。
	これを収束するまで繰り返して、互いの最適な選択の確率分布を求める。
	
	ぷよぷよではあまりないことだと思うが、自分の選択Aが相手の選択Pに強くてQに弱く、
	自分の選択Bは相手の選択Pに弱くてQに強いということが起こった場合、
	自分がAを選ぶ確率を高くする→相手がQを選ぶ→自分がBを選ぶ→相手がPを選ぶ→自分がAを選ぶ…と、
	確率分布が収束せずに振動することがある。
	このときは振動周期を推定して直近1周期分の平均を取る。
	
	また、評価値の範囲は－∞～∞であり、これと確率分布との積をとっても意味がないので、
	f(x) = 1 / (1 + e^(-x)) を評価値に適用して0~1の範囲に収め、これを予想勝率とする。
	確率分布から計算された最終的な予想勝率にはf^(-1)(x) = log(x / (1 - x)) を適用して評価値に戻す。
	*/
	
	public double[][] evaluationmatrix; // 評価値の行列
	public double[][] winningratematrix; // 予想勝率の行列
	public int mymovenum; // 自分の行える手の数
	public int mmm1; // mymovenum - 1
	public int enemymovenum; // 相手の行える手の数
	public double[][] mychoiceprobs; // 自分の各手の選択確率。振動したときの対策用に過去数回の履歴を残す
	public double[] enemychoiceprobs; // 自分の各手の選択確率。これは履歴を残さない
	public double[] mywinningrates; // 自分が各手を選択したときの予想勝率
	public double[] enemywinningrates; // 相手が各手を選択したときの相手の予想勝率
	public double[] mychoiceweights; // 自分の各手の良さ
	public double[] enemychoiceweights; // 相手の各手の（相手にとっての）良さ
	public double myweightsum;
	public double enemyweightsum;
	public double mywinningrate;
	public double evaluationvalue;
	
	public void Settings(double[][] evamat) {
		evaluationmatrix = evamat;
		mymovenum = evaluationmatrix.length;
		mmm1 = mymovenum - 1;
		enemymovenum = evaluationmatrix[0].length;
		winningratematrix = new double[mymovenum][enemymovenum];
		mychoiceprobs = new double[mymovenum][mymovenum];
		for (int i=0;i<mymovenum;i++) {
			mychoiceprobs[mmm1][i] = 1.0 / mymovenum;
		}
		enemychoiceprobs = new double[enemymovenum];
		mywinningrates = new double[mymovenum];
		enemywinningrates = new double[enemymovenum];
		mychoiceweights = new double[mymovenum];
		enemychoiceweights = new double[enemymovenum];
	}
	
	public void Main() {
		MakeMatrix();
		for (int i=0;i<mymovenum;i++) {
			Calc();
		}
		int cycle = EstimateCycle();
		double[] bestchoice = BestChoiceProbs(cycle);
		mychoiceprobs[mmm1] = bestchoice;
		Calc();
		Evaluate();
	}
	
	public void MakeMatrix() {
		for (int i=0;i<mymovenum;i++) {
			for (int j=0;j<enemymovenum;j++) {
				winningratematrix[i][j] = 1 / (1 + Math.exp(-evaluationmatrix[i][j]));
			}
		}
	}
	
	public void Calc() {
		// 自分の選択の確率分布をもとに相手の最適な選択の確率分布を求める
		for (int j=0;j<enemymovenum;j++) {
			double tempwinningrate = 0;
			for (int i=0;i<mymovenum;i++) {
				tempwinningrate += mychoiceprobs[mmm1][i] * winningratematrix[i][j];
			}
			enemywinningrates[j] = 1 - tempwinningrate;
			if (enemywinningrates[j] == 1) {
				enemychoiceweights[j] = 30000;
			}
			else {
				enemychoiceweights[j] = enemywinningrates[j] / (1 - enemywinningrates[j]);
			}
			enemyweightsum += enemychoiceweights[j];
		}
		for (int j=0;j<enemymovenum;j++) {
			enemychoiceprobs[j] = enemychoiceweights[j] / enemyweightsum;
		}
		
		// 相手の選択の確率分布をもとに自分の選択の確率分布を修正する
		for (int i=0;i<mymovenum;i++) {
			double tempwinningrate = 0;
			for (int j=0;j<enemymovenum;j++) {
				tempwinningrate += enemychoiceprobs[j] * winningratematrix[i][j];
			}
			mywinningrates[i] = tempwinningrate;
			if (mywinningrates[i] == 1) {
				mychoiceweights[i] = 30000;
			}
			else {
				mychoiceweights[i] = mywinningrates[i] / (1 - mywinningrates[i]);
			}
			myweightsum += mychoiceweights[i];
		}
		for (int i=1;i<mymovenum;i++) {
			mychoiceprobs[i - 1] = Arrays.copyOf(mychoiceprobs[i], mymovenum);
		}
		for (int i=0;i<mymovenum;i++) {
			mychoiceprobs[mmm1][i] = mychoiceweights[i] / myweightsum;
		}
	}
	
	public double Distance(double[] x, double[] y) {
		if (x.length == 0 || y.length == 0) {
			return 0;
		}
		else {
			int veclen = Math.min(x.length, y.length);
			double tempsum = 0;
			for (int i=0;i<veclen;i++) {
				tempsum += (x[i] - y[i]) * (x[i] - y[i]);
			}
			return Math.sqrt(tempsum / veclen);
		}
	}
	
	public int EstimateCycle() {
		// 確率分布の２乗誤差をもとに確率分布の振動の周期を推定する
		double[] distances = new double[mmm1];
		for (int i=0;i<mmm1;i++) {
			distances[i] = Distance(mychoiceprobs[i], mychoiceprobs[mmm1]);
		}
		double sumdistance = 0;
		double sumdistancepow2 = 0;
		for (int i=0;i<mmm1;i++) {
			sumdistance += distances[i];
			sumdistancepow2 += distances[i] * distances[i];
		}
		if (mmm1 == 0) {
			return 1;
		}
		else {
			double averagedistance = sumdistance / mmm1;
			double stddistance = Math.sqrt(averagedistance * averagedistance - sumdistancepow2 / mmm1);
			double threshold = averagedistance - stddistance * 0.5;
			int cycle = 1;
			for (int i=1;i<=mmm1;i++) {
				if (distances[mmm1-i] <threshold) {
					cycle = i;
					break;
				}
			}
			return cycle;
		}
	}
	
	public double[] BestChoiceProbs(int cycle) {
		double[] output = new double[mymovenum];
		for (int i=0;i<mymovenum;i++) {
			double tempprobsum = 0;
			for (int j=0;j<cycle;j++) {
				tempprobsum += mychoiceprobs[mmm1-j][i];
			}
			output[i] = tempprobsum / cycle;
		}
		return output;
	}
	
	public void Evaluate() {
		double tempwinningrate = 0;
		for (int i=0;i<mymovenum;i++) {
			tempwinningrate += mychoiceprobs[mmm1][i] * mywinningrates[i];
		}
		mywinningrate = tempwinningrate;
		if (mywinningrate == 0) {
			evaluationvalue = -30000;
		}
		else if (mywinningrate == 1) {
			evaluationvalue = 30000;
		}
		else {
			evaluationvalue = Math.log(mywinningrate / (1 - mywinningrate));
		}
		if (evaluationvalue != evaluationvalue) {
			// evaluationvalueがNaNになってしまったら
			evaluationvalue = 0;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
}
