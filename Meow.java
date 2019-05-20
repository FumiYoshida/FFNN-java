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


public class Meow extends AbstractSamplePlayer {
	
	public boolean isfirst = true;
	public boolean issecond = false;
	public boolean isthird = false;
	public FirstMove fm;
	// TA3相手に勝率66%, maou相手に勝率50%、Ultimate相手に勝率26%くらい
	
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
		int width = field.getWidth();
		int height = field.getHeight();
		int myactionnum = 0;
		int grace = 1;
		int scorethreshold = 1000;
		int scorethreshold2 = 300;
		int scorethreshold3 = 1000;
		int ojamathreshold = 0;
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
		BitNextField bnf = new BitNextField();
		if (grace < 3 && board.getTotalNumberOfOjama() >= ojamathreshold) {
			/* 2ターン以内（最長でもこのターンの次のターンが終わったとき）
			 *  におじゃまが降ってくるとき、おじゃまが降る前に連鎖を発火させる。
			 *  発火できないときはカウンター形を築く。
			 */
			bnf.ReadField(field);
			bnf.Calc(true, false, iscurpuyosame, curf, curs);
			long[][] firstfields = DeepCopy(bnf.fieldafteravailableactions);
			long[] firstojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
			myactionnum = bnf.availableactions.length;
			int[] maxsumscores = new int[myactionnum];
			int[] firstscores = new int[myactionnum];
			int[][] savedactions = DeepCopy(bnf.availableactions);
			
			if (grace == 1) {
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i], firstojamafield, false, false, isnexpuyosame, nexf, nexs);
					maxsumscores[i] = bnf.score;
				}
			}
			else if (grace == 2) {
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i], firstojamafield, true, false, isnexpuyosame, nexf, nexs);
					long[][] secondfields = bnf.fieldafteravailableactions;
					long[] secondojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
					firstscores[i] = bnf.score;
					for (int j=0;j<secondfields.length;j++) {
						bnf.Calc(secondfields[j], secondojamafield, false, false, isnexnexpuyosame, nexnexf, nexnexs);
						int temp = firstscores[i] + bnf.score;
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
			System.out.println(Arrays.deepToString(savedactions));
			System.out.println(myactionnum);
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
			
			// 相手が3ターン以内に発火できる連鎖を調べる
			bnf.ReadField(getEnemyBoard().getField());
			bnf.Calc(true, false, iscurpuyosame, curf, curs);
			int enemyactionnum = bnf.availableactions.length;
			int[] enemymaxsumscores = new int[enemyactionnum];
			long[][] enemyfirstfields = DeepCopy(bnf.fieldafteravailableactions);
			long[] enemyfirstojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
			for (int i=0;i<enemyactionnum;i++) {
				bnf.Calc(enemyfirstfields[i], enemyfirstojamafield, true, false, isnexpuyosame, nexf, nexs);
				long[][] enemysecondfields = DeepCopy(bnf.fieldafteravailableactions);
				long[] enemysecondojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
				int tempfirstscore = bnf.score;
				for (int j=0;j<enemysecondfields.length;j++) {
					bnf.Calc(enemysecondfields[j], enemysecondojamafield, true, false, isnexnexpuyosame, nexnexf, nexnexs);
					long[][] enemythirdfields = DeepCopy(bnf.fieldafteravailableactions);
					long[] enemythirdojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
					int tempsecondscore = bnf.score;
					for (int k=0;k<enemythirdfields.length;k++) {
						bnf.Calc(enemythirdfields[k], enemythirdojamafield, false, false, false, 0, 0);
						int tempthirdscore = bnf.score;
						int temp = tempfirstscore + tempsecondscore + tempthirdscore;
						if (temp > enemymaxsumscores[i]) {
							enemymaxsumscores[i] = temp;
						}
					}
				}
			}
			int enemymaxscore = 0;
			for (int i=0;i<enemyactionnum;i++) {
				if (enemymaxsumscores[i] > enemymaxscore) {
					enemymaxscore = enemymaxsumscores[i];
				}
			}
			bnf.ReadField(field);
			bnf.Calc(true, false, iscurpuyosame, curf, curs);
			long[][] firstfields = DeepCopy(bnf.fieldafteravailableactions);
			long[] firstojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
			myactionnum = bnf.availableactions.length;
			int[] maxsumscores = new int[myactionnum];
			int[] firstscores = new int[myactionnum];
			int[][] savedactions = DeepCopy(bnf.availableactions);
			
			
			for (int i=0;i<myactionnum;i++) {
				bnf.Calc(firstfields[i], firstojamafield, true, false, isnexpuyosame, nexf, nexs);
				long[][] mysecondfields = DeepCopy(bnf.fieldafteravailableactions);
				long[] mysecondojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
				int tempfirstscore = bnf.score;
				firstscores[i] = bnf.score;
				for (int j=0;j<mysecondfields.length;j++) {
					bnf.Calc(mysecondfields[j], mysecondojamafield, true, false, isnexnexpuyosame, nexnexf, nexnexs);
					long[][] mythirdfields = DeepCopy(bnf.fieldafteravailableactions);
					long[] mythirdojamafield = Arrays.copyOf(bnf.ojamafield, bnf.ojamafield.length);
					int tempsecondscore = bnf.score;
					for (int k=0;k<mythirdfields.length;k++) {
						bnf.Calc(mythirdfields[k], mythirdojamafield, false, true, false, 0, 0);
						int tempthirdscore = bnf.score;
						int tempfirepossibility = bnf.firepossibility;					
						// int temp = (int)(tempfirstscore * 1.1 + tempsecondscore * 1.05 + tempthirdscore + tempfirepossibility * 0);
						int temp = (int)(tempfirstscore * 1.1 + tempsecondscore * 1.05 + tempthirdscore + tempfirepossibility * 0.3 * (0.5 + Math.exp(1 - bnf.numtofire)));
						if (temp > maxsumscores[i]) {
							maxsumscores[i] = temp;
						}
					}
				}
			}
			int mypotential = new FirePossibility().Calc(field);
			// int enemypoint = new FirePossibility().Calc(getEnemyBoard().getField());

			
			
			int maxscore = 0;
			int selectindex = 0;
			int firstmaxscore = 0;
			int firstmaxindex = 0;
			for (int i=0;i<myactionnum;i++) {
				if (maxsumscores[i] > maxscore) {
					maxscore = maxsumscores[i];
					selectindex = i;
				}
				if (firstscores[i] > firstmaxscore) {
					firstmaxscore = firstscores[i];
					firstmaxindex = i;
				}
			}

			System.out.println("enemy's max score is : " + enemymaxscore);
			System.out.println("my max score is : " + maxscore);
			System.out.println("my max score using current puyo is : " + firstmaxscore);
			

			long end = System.currentTimeMillis();
			System.out.println(end - start);
			if (firstmaxscore > enemymaxscore + 1000) {
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
							if (firstscores[i] == 0 && maxsumscores[i] > nextturnmaxscore) {
								nextturnmaxscore = maxsumscores[i];
								tempindex = i;
							}
						}
						PuyoDirection selectdirection = PuyoDirection.values()[savedactions[tempindex][0]];
						int selectcolumn = savedactions[tempindex][1];
						myaction = new Action(selectdirection, selectcolumn);
					}
					else if (maxscore < scorethreshold) {
						// あまりにも小さい発火だったら
						System.out.println(maxscore);
						System.out.println(mypotential);
						// すぐには発火させないものをなるたけ選んでいる
						int[][] scores = new int[myactionnum][2];
						for (int i=0;i<myactionnum;i++) {
							scores[i][0] = firstscores[i];
							scores[i][1] = maxsumscores[i];
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
	
	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new Meow();
		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
