package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.GameInfo.PlayerNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;
import sp.AbstractSamplePlayer;


public class FastMeow4 extends AbstractSamplePlayer {
	
	public boolean isfirst = true;
	public boolean issecond = false;
	public boolean isthird = false;
	public FirstMove fm;
	// TA3相手に勝率60%くらい
	
	@Override
	public Action doMyTurn() {
		long start = System.currentTimeMillis();
		Board board = getMyBoard();
		isfirst = true;
		for (int i=0;i<6;i++) {
			if (board.getField().getTop(i) >= 0) {
				isfirst = false;
			}
		}
		if (isfirst) {
			isfirst = false;
			board.getCurrentPuyo().rotate();
			return new Action(board.getCurrentPuyo(), 2);
		}
		Field field = board.getField();
		Puyo curpuyo = board.getCurrentPuyo();
		Puyo nexpuyo = board.getNextPuyo();
		Puyo nexnexpuyo = board.getNextNextPuyo();
		List<Integer> ojal = board.getNumbersOfOjamaList();
		int myactionnum = 0;
		int grace = 1;
		int scorethreshold = 1000;
		int ojamathreshold = 4;
		for (int oja : ojal) {
			if (oja > 0) {
				break;
			}
			grace++;
		}
		Action myaction = null;
		int curf = ReadPuyoType(curpuyo.getPuyoType(PuyoNumber.FIRST));
		int curs = ReadPuyoType(curpuyo.getPuyoType(PuyoNumber.SECOND));
		int nexf = ReadPuyoType(nexpuyo.getPuyoType(PuyoNumber.FIRST));
		int nexs = ReadPuyoType(nexpuyo.getPuyoType(PuyoNumber.SECOND));
		int nexnexf = ReadPuyoType(nexnexpuyo.getPuyoType(PuyoNumber.FIRST));
		int nexnexs = ReadPuyoType(nexnexpuyo.getPuyoType(PuyoNumber.SECOND));
		boolean iscurpuyosame = curf == curs;
		boolean isnexpuyosame = nexf == nexs;
		boolean isnexnexpuyosame = nexnexf == nexnexs;
		FastBitNextField bnf = new FastBitNextField();
		if (grace < 3 && board.getTotalNumberOfOjama() >= ojamathreshold) {
			/* 2ターン以内（最長でもこのターンの次のターンが終わったとき）
			 *  におじゃまが降ってくるとき、おじゃまが降る前に連鎖を発火させる。
			 *  発火できないときはカウンター形を築く。
			 */
			FieldInfo firstfield = bnf.ReadField(board);
			bnf.Calc(firstfield, true, false);
			FieldInfo[] firstfields = firstfield.AvailableFields(nexf, nexs);
			myactionnum = firstfields.length;
			int[] maxsumscores = new int[myactionnum];
			int[] firstscores = new int[myactionnum];
			int[][] savedactions = firstfield.availableactions;
			if (grace == 1) {
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i],  false, false);
					maxsumscores[i] = firstfields[i].score;
				}
			}
			else if (grace == 2) {
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i], true, false);
					firstscores[i] = firstfields[i].score;
					FieldInfo[] secondfields = firstfields[i].AvailableFields(nexnexf, nexnexs);
					for (int j=0;j<secondfields.length;j++) {
						bnf.Calc(secondfields[j], false, false);
						int temp = firstscores[i] + secondfields[j].score;
						if (temp > maxsumscores[i]) {
							maxsumscores[i] = temp;
						}
					}
				}
			}
			int maxscore = 0;
			int selectindex = 0;
			for (int i=0;i<myactionnum;i++) {
				if (maxsumscores[i] > maxscore) {
					maxscore = maxsumscores[i];
					selectindex = i;
				}
			}
			if (myactionnum == 0) {
				new ASCIIArt().DefaultFox();
				myaction = new Action(PuyoDirection.DOWN, 0);
			}
			else if (myaction == null) {
				PuyoDirection selectdirection = PuyoDirection.values()[savedactions[selectindex][0]];
				int selectcolumn = savedactions[selectindex][1];
				myaction = new Action(selectdirection, selectcolumn);
			}
		}
		else {
			/* まだしばらくは猶予があるとき
			 *  3ターン以内（ネクネクまで使ったとき）に、もしくは
			 *  望ましい色が1つでも来れば4ターン以内発火させることが可能な手
			 * （つまりは発火点をつぶさない手）の内、その発火する連鎖数が最も大きい手を選ぶ。
			 */
			
			// 相手が4ターン以内に発火できる連鎖を調べる
			FieldInfo enemyfirstfield = bnf.ReadField(getEnemyBoard());
			bnf.Calc(enemyfirstfield, true, false);
			FieldInfo[] enemyfirstfields = enemyfirstfield.AvailableFields(nexf, nexs);
			int enemyactionnum = enemyfirstfields.length;
			int[] enemymaxsumscores = new int[enemyactionnum];
			int enemyfirstscore = 0;
			for (int i=0;i<enemyactionnum;i++) {
				bnf.Calc(enemyfirstfields[i], true, false);
				FieldInfo[] enemysecondfields = enemyfirstfields[i].AvailableFields(nexnexf, nexnexs);
				int tempfirstscore = enemyfirstfields[i].score;
				enemyfirstscore = Math.max(enemyfirstscore, tempfirstscore);
				for (int j=0;j<enemysecondfields.length;j++) {
					bnf.Calc(enemysecondfields[j], true, false);
					FieldInfo[] enemythirdfields = enemysecondfields[j].AvailableFields(0, 0);
					int tempsecondscore = enemysecondfields[j].score;
					for (int k=0;k<enemythirdfields.length;k++) {
						bnf.CalcEnemy(enemythirdfields[k]);
						int tempthirdscore = enemythirdfields[k].score;
						double averagefourthscore = bnf.Tsumos(enemythirdfields[k], true);
						/*
						double averagefourthscore = 0;
						for (int l=0;l<15;l++) {
							// 各つもについて見る
							FieldInfo enemyfourthfield = bnf.Tsumos(enemythirdfields[k], l);
							FieldInfo[] enemyfourthfields = enemyfourthfield.AvailableFields(0, 0);
							double tempfourthscore = 0; 
							for (int m=0;m<enemyfourthfields.length;m++) {
								bnf.Calc(enemyfourthfields[m], false, false);
								tempfourthscore = Math.max(tempfourthscore, enemyfourthfields[m].score);
							}
							averagefourthscore += tempfourthscore / 15;
						}
						*/
						double tempfourtheva = tempfirstscore  + tempsecondscore + tempthirdscore + averagefourthscore;
						enemymaxsumscores[i] = Math.max(enemymaxsumscores[i], (int)tempfourtheva);
					}
				}
			}
			int enemymaxscore = 0;
			for (int i=0;i<enemyactionnum;i++) {
				if (enemymaxsumscores[i] > enemymaxscore) {
					enemymaxscore = enemymaxsumscores[i];
				}
			}
			
			int[][] columnbias = new int[4][6];
			for (int i=0;i<3;i++)
			{
				columnbias[0][i] = i;
				columnbias[0][5-i] = i;
				columnbias[2][i] = i;
				columnbias[2][5-i] = i;
				columnbias[1][i] = i;
				columnbias[1][5-i] = i;
				columnbias[3][Math.max(0, i-1)] = i;
				columnbias[1][Math.min(5, 5-i+1)] = i;
			}
			int multi = iscurpuyosame ? 2 : 1;
			multi = isnexpuyosame ? multi * 2 : multi;
			multi = isnexnexpuyosame ? multi * 2 : multi;
			long start2 = System.currentTimeMillis();
			System.out.println((start2 - start) * multi);
			
			
			// 4手先まで読む
			FieldInfo myfirstfield = bnf.ReadField(board);
			bnf.Calc(myfirstfield, true, false);
			FieldInfo[] myfirstfields = myfirstfield.AvailableFields(nexf, nexs);
			myactionnum = myfirstfields.length;
			double[] mymaxsumscores = new double[myactionnum];
			int[] firstscores = new int[myactionnum];
			int[][] savedactions = myfirstfield.availableactions;
			for (int i=0;i<myactionnum;i++) {
				bnf.Calc(myfirstfields[i], true, false);
				FieldInfo[] mysecondfields = myfirstfields[i].AvailableFields(nexnexf, nexnexs);
				int tempfirstscore = myfirstfields[i].score;
				firstscores[i] =  tempfirstscore;
				// tempfirstscore += columnbias[savedactions[i][0]][savedactions[i][1]] * 20;
				for (int j=0;j<mysecondfields.length;j++) {
					bnf.Calc(mysecondfields[j], true, false);
					FieldInfo[] mythirdfields = mysecondfields[j].AvailableFields(0, 0);
					int tempsecondscore = mysecondfields[j].score;
					double[] thirdevaluations = new double[mythirdfields.length];
					int[] thirdscores = new int[mythirdfields.length];
					for (int k=0;k<mythirdfields.length;k++) {
						bnf.CalcEnemy(mythirdfields[k]);
						int tempthirdscore = mythirdfields[k].score;
						double thirdpossibility = mythirdfields[k].firepossibility;
						double averagefourthscore = bnf.Tsumos(mythirdfields[k], false);
						double tempfourtheva = tempfirstscore * 1.15  + tempsecondscore * 1.1 + tempthirdscore * 1.05 + averagefourthscore + thirdpossibility * 1.5;
						mymaxsumscores[i] = Math.max(mymaxsumscores[i], tempfourtheva);
					}
				}
			}
			int mypotential = new FirePossibility().Calc(field);
			// int enemypoint = new FirePossibility().Calc(getEnemyBoard().getField());

			
			
			double maxscore = 0;
			int selectindex = 0;
			int firstmaxscore = 0;
			int firstmaxindex = 0;
			for (int i=0;i<myactionnum;i++) {
				if (mymaxsumscores[i] > maxscore) {
					maxscore = mymaxsumscores[i];
					selectindex = i;
				}
				if (firstscores[i] > firstmaxscore) {
					firstmaxscore = firstscores[i];
					firstmaxindex = i;
				}
			}
			/*
			System.out.println("enemy's max score is : " + enemymaxscore);
			System.out.println("my max score is : " + maxscore);
			System.out.println("my max score using current puyo is : " + firstmaxscore);
			*/

			long end = System.currentTimeMillis();
			System.out.println((end - start2)*multi);
			double multi2 = isnexnexpuyosame ? multi / 12 * 22 : (double)multi / 6 * 22;
			System.out.println((double)(end - start2) * multi2);
			System.out.println("--------------------------------");
			if (firstmaxscore > enemymaxscore + 1000 && board.getTotalNumberOfOjama() < ojamathreshold) {
				// 相手の組んでいる連鎖が小さくて速攻で倒せそうだったら
				new ASCIIArt().ThiefFox();
				PuyoDirection selectdirection = PuyoDirection.values()[savedactions[firstmaxindex][0]];
				int selectcolumn = savedactions[firstmaxindex][1];
				myaction = new Action(selectdirection, selectcolumn);
			}
			if (myactionnum == 0) {
				new ASCIIArt().DefaultFox();
				myaction = new Action(PuyoDirection.DOWN, 0);
			}
			else if (myaction == null) {
				if (firstscores[selectindex] > 0) {
					// 発火させる決断をする直前
					if (firstscores[selectindex] < enemymaxscore) {
						// 発火してもおじゃま返されるようだったら
						int nextturnmaxscore = 0;
						int tempindex = 0;
						for (int i=0;i<myactionnum;i++) {
							if (firstscores[i] == 0 && mymaxsumscores[i] > nextturnmaxscore) {
								nextturnmaxscore =(int) mymaxsumscores[i];
								tempindex = i;
							}
						}
						PuyoDirection selectdirection = PuyoDirection.values()[savedactions[tempindex][0]];
						int selectcolumn = savedactions[tempindex][1];
						myaction = new Action(selectdirection, selectcolumn);
					}
					else if (maxscore < scorethreshold) {
						// あまりにも小さい発火だったら
						// すぐには発火させないものをなるたけ選んでいる
						int[][] scores = new int[myactionnum][2];
						for (int i=0;i<myactionnum;i++) {
							scores[i][0] = firstscores[i];
							scores[i][1] = (int) mymaxsumscores[i];
						}
						myaction = Sample08(savedactions, scores);
					}
				}
				if (myaction == null) {
					PuyoDirection selectdirection = PuyoDirection.values()[savedactions[selectindex][0]];
					int selectcolumn = savedactions[selectindex][1];
					myaction = new Action(selectdirection, selectcolumn);
				}
			}
		}
		return myaction;
	}
	
	public Action MaxPotential(Puyo[] availablepuyos, int[] availablecolumns, int[] potential) {
		int maxpot = 0;
		int maxpotindex = 0;
		for (int i=0;i<availablecolumns.length;i++) {
			if (potential[i] > maxpot) {
				maxpot = potential[i];
				maxpotindex = i;
			}
		}
		return new Action(availablepuyos[maxpotindex], availablecolumns[maxpotindex]);
	}
	
	public Action Sample08(int[][] availableactions, int[][] scores) {
		System.out.println("used sample player 08");
		Action action = null;
		Field field = getMyBoard().getField();
		Puyo puyo = getMyBoard().getCurrentPuyo();
		int maxNeighborPuyo = 0;
		int scoreofmnp = 0;
		for(int i = 0; i < field.getWidth(); i++){
			for(PuyoDirection dir:PuyoDirection.values()){
				int index = IndexOfAction(dir, i, availableactions);
				int tempscore = 0;
				if (index >= 0) {
					tempscore = scores[index][1];
					if (scores[index][0] != 0) {
						// すぐに発火するものは選ばない
						continue;
					}
				}
				if(!isEnable(dir, i)){
					continue;
				}
				PuyoType firstPuyo = puyo.getPuyoType(PuyoNumber.FIRST);
				PuyoType secondPuyo = puyo.getPuyoType(PuyoNumber.SECOND);
				int firstNeighbor = 0;
				int secondNeighbor = 0;

				//最初のぷよの周りに存在する同色ぷよ数を数える
				if(dir == PuyoDirection.DOWN){
					//二番目のぷよが下にある場合は，topの二つ上がy座標
					int y = field.getTop(i)+2;
					firstNeighbor = getNeighborPuyoNum(i, y, firstPuyo);
				}
				else{
					//二番目のぷよが下にある場合以外は，topの1つ上がy座標
					int y = field.getTop(i)+1;
					firstNeighbor = getNeighborPuyoNum(i, y, firstPuyo);
				}

				//二番目のぷよの周りに存在する同色ぷよを数える
				if(dir == PuyoDirection.DOWN){
					//二番目のぷよが下にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.UP){
					//二番目のぷよが上にある場合
					int y = field.getTop(i)+2; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.RIGHT){
					//二番目のぷよが右にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.LEFT){
					//二番目のぷよが左にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				int fsn = firstNeighbor + secondNeighbor;
				if (fsn == maxNeighborPuyo && tempscore > scoreofmnp) {
					// 3ターン後に得ることが可能な点数が高い方を優先する
					scoreofmnp = tempscore;
					action = new Action(dir, i);
				}
				if (fsn > maxNeighborPuyo) {
					maxNeighborPuyo = fsn;
					scoreofmnp = tempscore;
					action = new Action(dir, i);
				}
			}
		}
		return action;
	}
	
	private int IndexOfAction(PuyoDirection dir, int column, int[][] availableactions) {
		for (int i=0;i<availableactions.length;i++) {
			if (dir == PuyoDirection.values()[availableactions[i][0]] && column == availableactions[i][1]) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean isEnable(PuyoDirection dir, int i) {
		Field field = getMyBoard().getField();
		if(!field.isEnable(dir, i)){
			return false;
		}
		if(dir == PuyoDirection.DOWN || dir == PuyoDirection.UP){
			if(field.getTop(i) >= field.getDeadLine()-2){
				return false;
			}
		}
		else if(dir == PuyoDirection.RIGHT){
			if(field.getTop(i) >= field.getDeadLine()-2 || field.getTop(i+1) >= field.getDeadLine()-2) {
				return false;
			}
		}
		else if(dir == PuyoDirection.LEFT){
			if(field.getTop(i) >= field.getDeadLine()-2 || field.getTop(i-1) >= field.getDeadLine()-2) {
				return false;
			}
		}
		return true;
	}

	private int getNeighborPuyoNum(int x, int y, PuyoType puyoType) {
		//数を記録する変数
		int count = 0;
		Field field = getMyBoard().getField();	
		if (x - 1 >= 0) {
			if (field.getPuyoType(x-1, y) == puyoType) {
				count++;
			}
		}
		if (x + 1 < field.getWidth()) {
			if (field.getPuyoType(x+1, y) == puyoType) {
				count++;
			}
		}
		if (y - 1 >= 0) {
			if (field.getPuyoType(x, y-1) == puyoType) {
				count++;
			}
		}
		if (y + 1 < field.getHeight()) {
			if (field.getPuyoType(x, y+1) == puyoType) {
				count++;
			}
		}
		return count;
	}
	
	public int CountBlank() {
		Field f = getMyBoard().getField();
		int width = f.getWidth();
		int height = f.getHeight() - 2;
		int output = 0;
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (f.getPuyoType(i, j) == null) {
					output++;
				}
			}
		}
		return output;
	}
	
	public int ReadPuyoType(PuyoType puyotype) {
		if (puyotype == null) {
			return 0;
		}
		else {
			switch (puyotype) {
			case BLUE_PUYO:
				return 1;
			case GREEN_PUYO:
				return 2;
			case PURPLE_PUYO:
				return 3;
			case RED_PUYO:
				return 4;
			case YELLOW_PUYO:
				return 5;
			default:
				return 0;
			}
		}
	}
	
	public int[][] DeepCopy(int[][] input){
		int[][] output = new int[input.length][];
		for (int i=0;i<input.length;i++) {
			output[i] = Arrays.copyOf(input[i], input[i].length);
		}
		return output;
	}
	
	public long[][] DeepCopy(long[][] input){
		long[][] output = new long[input.length][];
		for (int i=0;i<input.length;i++) {
			output[i] = Arrays.copyOf(input[i], input[i].length);
		}
		return output;
	}
	
	private int[] IndexofSorted(double[] input) {
		int[] output = new int[input.length];
		for (int i=0;i<input.length;i++) {
			output[i] = i;
		}
		quicksort(Arrays.copyOf(input, input.length), output, 0, input.length-1);
		return output;
	}
	
	/* x, y, z の中間値を返す */
	private double med3(double x, double y, double z) {
	    if (x < y) {
	        if (y < z) return y; else if (z < x) return x; else return z;
	    } else {
	        if (z < y) return y; else if (x < z) return x; else return z;
	    }
	}

	/* クイックソート
	 * a     : ソートする配列
	 * left  : ソートするデータの開始位置
	 * right : ソートするデータの終了位置
	 */
	private void quicksort(double[] a, int[] index, int left, int right) {
	    if (left < right) {
	        int i = left;
	        int j = right;
	        double pivot = med3(a[i], a[(i + j) / 2], a[j]); 
	        while (true) { /* a[] を pivot 以上と以下の集まりに分割する */
	            while (a[i] < pivot) {
	            	i++; /* a[i] >= pivot となる位置を検索 */
	            }
	            while (pivot < a[j]) { 
	            	j--; /* a[j] <= pivot となる位置を検索 */
	            }
	            if (i >= j) {
	            	break;
	            }
	            double tmp = a[i]; 
	            a[i] = a[j]; 
	            a[j] = tmp; /* a[i], a[j] を交換 */
	            int tmpi = index[i];
	            index[i] = index[j];
	            index[j] = tmpi;
	            i++;
	            j--;
	        }
	        quicksort(a, index, left, i - 1);  /* 分割した左を再帰的にソート */
	        quicksort(a, index, j + 1, right); /* 分割した右を再帰的にソート */
	    }
	}
	
	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new FastMeow4();
		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}