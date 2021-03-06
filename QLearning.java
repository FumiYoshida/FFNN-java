package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class QLearning {
	Puyo[] availablemypuyos;
	Puyo[] availableenemypuyos;
	int[] availablemypuyocolumns;
	int[] availableenemypuyocolumns;
	Action bestaction;
	
	
	int colornum = 5;
	int cnp1; // おじゃまも含めたぷよの種類の数
	int width;
	int height;
	int maxojamalistnum = 10;
	int inputlen;
	int[] layers = {-1, 200, 200, 1};
	FFNN nn;
	double epsilon = 0.5;
	double gamma = 0.95;
	
	public void FFNNSettings(Board board) {
		// これは初回に呼べばよい
		cnp1 = colornum + 1;
		width = board.getField().getWidth();
		height = board.getField().getHeight()-1; // 12段目まで埋まっていて縦に置いたときにIndexOutofRangeにならないよう修正する
		inputlen =  width * height * cnp1 * 2 + maxojamalistnum * 2 + 2 + colornum * 6;
		layers[0] = inputlen;
		nn = new FFNN();
		nn.layers = layers;
		nn.firstinput = new double[1][inputlen];
		nn.Settings();
	}
	
	public Action Main(Board board, PlayerInfo me, Board enemyboard, PlayerInfo enemy) {
		// この関数を行動毎に呼ぶ
		MakeAllActions(board, enemyboard);
		int myactionnum = availablemypuyos.length;
		int enemyactionnum = availableenemypuyos.length;
		NextField[] mynexts = new NextField[myactionnum];
		NextField[] enemynexts = new NextField[enemyactionnum];
		int[] sprungenemyojama = new int[enemyactionnum];
		double[][] evaluationmatrix = new double[myactionnum][enemyactionnum];
		for (int i=0;i<enemyactionnum;i++) {
			enemynexts[i] = new NextField();
			enemynexts[i].Settings(availableenemypuyos[i], availableenemypuyocolumns[i], enemyboard, enemy, board);
			sprungenemyojama[i] = enemynexts[i].myscore;
		}
		for (int i=0;i<myactionnum;i++) {
			mynexts[i] = new NextField();
			mynexts[i].Settings(availablemypuyos[i], availablemypuyocolumns[i], board, me, enemyboard);
			if (mynexts[i].IsAlive(mynexts[i].field)) {
				for (int j=0;j<enemyactionnum;j++) {
					List<Integer>[] tempojama = mynexts[i].CalcOjama(sprungenemyojama[j]);
					int[][] tempfield = mynexts[i].RainDownOjama(tempojama[0]);
					int[][] tempenemyfield = enemynexts[j].RainDownOjama(tempojama[1]); // RainDownを呼び出すとtempojama[1]が1ターン進む
					double[] tempinput = MakeInput(tempfield, tempenemyfield, tempojama[0], tempojama[1], mynexts[i].myscore, enemynexts[j].myscore, board.getNextPuyo(), board.getNextNextPuyo(), null);
					evaluationmatrix[i][j] = Evaluate(tempinput); // order最大
				}
			}
			else {
				// 13段目において連鎖が起こらず、ゲームオーバーとなる場合
				for (int j=0;j<enemyactionnum;j++) {
					evaluationmatrix[i][j] = -30000;
				}
			}
		}
		if (evaluationmatrix.length != 0) {
			Evaluation evaluator = new Evaluation();
			evaluator.Settings(evaluationmatrix);
			evaluator.Main();

			// 学習
			double teacher = evaluator.evaluationvalue;
			NextField converter = new NextField();
			int[][] myf = converter.FieldtoFieldMatrix(board.getField());
			int[][] enf = converter.FieldtoFieldMatrix(enemyboard.getField());
			double[] input = MakeInput(myf, enf, board.getNumbersOfOjamaList(), enemyboard.getNumbersOfOjamaList(), me.getOjamaScore(), enemy.getOjamaScore(), board.getCurrentPuyo(), board.getNextPuyo(), board.getNextNextPuyo());
			Teach(input, teacher * gamma);
			
			// 行動の選択
			double[] myc = evaluator.mychoiceprobs[evaluator.mychoiceprobs.length-1];
			double maxprob = myc[0];
			int maxindex = 0;
			for (int i=1;i<myc.length;i++) {
				if (myc[i] > maxprob) {
					maxindex = i;
					maxprob = myc[i];
				}
			}
			Random rd = new Random(System.currentTimeMillis());
			int selectindex = 0;
			if (rd.nextDouble() < epsilon) {
				selectindex = rd.nextInt(availablemypuyos.length);
			}
			else {
				selectindex = maxindex;
			}
			bestaction = new Action(availablemypuyos[selectindex], availablemypuyocolumns[selectindex]);
			return bestaction;
		}
		else {
			bestaction = new Action(PuyoDirection.DOWN, 0);
			return bestaction;
		}
	}
	
	
	public double[] MakeInput(int[][] myfield, int[][] enemyfield, List<Integer> myojamalist, List<Integer> enemyojamalist, int myscore, int enemyscore, Puyo curpuyo, Puyo nexpuyo, Puyo nexnexpuyo) {
		int colornum = 5;
		int cnp1 = colornum + 1; // おじゃまも含めたぷよの種類の数
		int width = myfield.length;
		int height = myfield[0].length;
		int maxojamalistnum = 10;
		int outlen = width * height * cnp1 * 2 + maxojamalistnum * 2 + 2 + colornum * 6;
		double[] output = new double[outlen];
		int serial = 0;
		// 自分の盤面のぷよの位置と種類を入れる
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (myfield[i][j] != 0) {
					if (myfield[i][j] > 6) {
						output[serial+5] = 10 / (myfield[i][j] - 6); 
					}
					else{
						output[serial+myfield[i][j]-1] = 10; 
					}
				}
				serial += cnp1;
			}
		}
		// 相手の盤面のぷよの位置と種類を入れる
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (enemyfield[i][j] != 0) {
					if (enemyfield[i][j] > 6) {
						output[serial+5] = 10 / (enemyfield[i][j] - 6); 
					}
					else{
						output[serial+enemyfield[i][j]-1] = 10; 
					}
				}
				serial += cnp1;
			}
		}
		// 互いに10ターン以内に降ってくるおじゃまの数を入れる
		for (int i=0;i<Math.min(myojamalist.size(), maxojamalistnum);i++) {
			output[serial] = myojamalist.get(i);
			serial++;
		}
		serial += Math.max(0, (maxojamalistnum - myojamalist.size()));
		for (int i=0;i<Math.min(enemyojamalist.size(), maxojamalistnum);i++) {
			output[serial] = enemyojamalist.get(i);
			serial++;
		}
		serial += Math.max(0, (maxojamalistnum - enemyojamalist.size()));
		output[serial] = myscore;
		output[serial+1] = enemyscore;
		serial+=2;
		// ツモを入れる
		EnterPuyoTypeWithoutOjama(curpuyo.getPuyoType(PuyoNumber.FIRST), output, serial);
		EnterPuyoTypeWithoutOjama(curpuyo.getPuyoType(PuyoNumber.SECOND), output, serial+colornum);
		EnterPuyoTypeWithoutOjama(nexpuyo.getPuyoType(PuyoNumber.FIRST), output, serial+colornum*2);
		EnterPuyoTypeWithoutOjama(nexpuyo.getPuyoType(PuyoNumber.SECOND), output, serial+colornum*3);
		if (nexnexpuyo == null) {
			for (int i=0;i<colornum*2;i++) {
				output[serial+colornum*4+i] = 2;
			}
		}
		else {
			EnterPuyoTypeWithoutOjama(nexnexpuyo.getPuyoType(PuyoNumber.FIRST), output, serial+colornum*4);
			EnterPuyoTypeWithoutOjama(nexnexpuyo.getPuyoType(PuyoNumber.SECOND), output, serial+colornum*5);
		}
		return output;
	}
	
	public void EnterPuyoTypeWithoutOjama(PuyoType input, double[] output, int index) {
		if (input != null) {
			switch (input) {
			case BLUE_PUYO:
				output[index] = 10;
				break;
			case GREEN_PUYO:
				output[index+1] = 10;
				break;
			case PURPLE_PUYO:
				output[index+2] = 10;
				break;
			case RED_PUYO:
				output[index+3] = 10;
				break;
			case YELLOW_PUYO:
				output[index+4] = 10;
				break;
			}
		}
	}
	
	public double Evaluate(double[] input) {
		double[][] tempinput =  new double[1][];
		tempinput[0] = input;
		nn.ChangeInput(tempinput);
		nn.ForwardPropagation();
		return nn.outputs[nn.layernum-1][0][0];
	}
	
	public void Teach(double[] input, double teacher) {
		double[][] tempinput =  new double[1][];
		tempinput[0] = input;
		nn.ChangeInput(tempinput);
		nn.teacher = new double[1][1];
		nn.teacher[0][0] = teacher;
		nn.Learn();
	}
	
	
	public void MakeAllActions(Board board, Board enemyboard) {
		// 自分の行動最大22通り、相手の行動最大22通り、ネクネクの次のぷよ15通り（同じ：5通り、異なる：10通り）で分岐
		// それぞれ独立なので共通部分はコピーする
		// まず自分と相手がどんな行動をとれるかを考える
		Field myfield = board.getField();
		Field enemyfield = enemyboard.getField();
		boolean[] myactions = new boolean[11];
		boolean[] enemyactions = new boolean[11];
		for (int i=0;i<6;i++) {
			// 縦に設置できるか
			myactions[i] = myfield.getTop(i) < 10;
			enemyactions[i] = enemyfield.getTop(i) < 10;
		}
		for (int i=0;i<5;i++) {
			// 横に設置できるか
			myactions[i+6] = myfield.getTop(i) < 11 && myfield.getTop(i+1) < 11;
			enemyactions[i+6] = enemyfield.getTop(i) < 11 && enemyfield.getTop(i+1) < 11;
		}
		int myactionnum = 0;
		int enemyactionnum = 0;
		for (int i=0;i<11;i++) {
			if (myactions[i]) {
				myactionnum++;
			}
			if (enemyactions[i]) {
				enemyactionnum++;
			}
		}
		Puyo curpuyo = board.getCurrentPuyo();
		PuyoType firstpuyo = curpuyo.getPuyoType(PuyoNumber.FIRST);
		PuyoType secondpuyo = curpuyo.getPuyoType(PuyoNumber.SECOND);
		Puyo[] puyos = MakePuyos(curpuyo);
		Puyo[] mypuyos;
		Puyo[] enemypuyos;
		int[] mysetcols;
		int[] enemysetcols;
		int myactionserial = 0;
		int enemyactionserial = 0;
		if (firstpuyo == secondpuyo) {
			mypuyos = new Puyo[myactionnum];
			enemypuyos = new Puyo[enemyactionnum];
			mysetcols = new int[myactionnum];
			enemysetcols = new int[enemyactionnum];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					mypuyos[myactionserial] = puyos[0];
					mysetcols[myactionserial] = i;
					myactionserial++;
				}
				if (enemyactions[i]) {
					enemypuyos[enemyactionserial] = puyos[0];
					enemysetcols[enemyactionserial] = i;
					enemyactionserial++;
				}
			}
			for (int i=1;i<6;i++) {
				if (myactions[i+5]) {
					mypuyos[myactionserial] = puyos[3];
					mysetcols[myactionserial] = i;
					myactionserial++;
				}
				if (enemyactions[i+5]) {
					enemypuyos[enemyactionserial] = puyos[3];
					enemysetcols[enemyactionserial] = i;
					enemyactionserial++;
				}
			}
		}
		else {
			// 設置するぷよの向きが2通り
			mypuyos = new Puyo[myactionnum*2];
			enemypuyos = new Puyo[enemyactionnum*2];
			mysetcols = new int[myactionnum*2];
			enemysetcols = new int[enemyactionnum*2];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					mypuyos[myactionserial] = puyos[0];
					mypuyos[myactionserial+1] = puyos[2];
					mysetcols[myactionserial] = i;
					mysetcols[myactionserial+1] = i;
					myactionserial += 2;
				}
				if (enemyactions[i]) {
					enemypuyos[enemyactionserial] = puyos[0];
					enemypuyos[enemyactionserial+1] = puyos[2];
					enemysetcols[enemyactionserial] = i;
					enemysetcols[enemyactionserial+1] = i;
					enemyactionserial += 2;
				}
			}
			for (int i=1;i<6;i++) {
				if (myactions[i+5]) {
					mypuyos[myactionserial] = puyos[1]; // RIGHT
					mypuyos[myactionserial+1] = puyos[3]; // LEFT
					mysetcols[myactionserial] = i-1; // 0 ~ 4
					mysetcols[myactionserial+1] = i; // 1 ~ 5
					myactionserial += 2;
				}
				if (enemyactions[i+5]) {
					enemypuyos[enemyactionserial] = puyos[1];
					enemypuyos[enemyactionserial+1] = puyos[3];
					enemysetcols[enemyactionserial] = i-1;
					enemysetcols[enemyactionserial+1] = i;
					enemyactionserial += 2;
				}
			}
		}
		availablemypuyos = mypuyos;
		availableenemypuyos = enemypuyos;
		availablemypuyocolumns = mysetcols;
		availableenemypuyocolumns = enemysetcols;
	}
	
	public Puyo[] MakePuyos(Puyo moto) {
		// 各方向のぷよを作成する
		// 設置できるcolumnnumはUP, DOWNで0~5, RIGHTで0~4, LEFTで1~5
		Map<PuyoNumber, PuyoType> puyotypesmap = moto.getPuyoTypesMap();
		Puyo uepuyo = new Puyo(puyotypesmap);
		uepuyo.setDirection(PuyoDirection.UP);
		Puyo migipuyo = new Puyo(puyotypesmap);
		migipuyo.setDirection(PuyoDirection.RIGHT);
		Puyo shitapuyo = new Puyo(puyotypesmap);
		shitapuyo.setDirection(PuyoDirection.DOWN);
		Puyo hidaripuyo = new Puyo(puyotypesmap);
		hidaripuyo.setDirection(PuyoDirection.LEFT);
		Puyo[] output = new Puyo[] {uepuyo, migipuyo, shitapuyo, hidaripuyo};
		return output;
	}

}
