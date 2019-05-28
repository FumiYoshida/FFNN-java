package player;

import java.util.Arrays;
import java.util.List;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
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
	// maou相手に勝率63%くらい
	
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
		int nexf = ReadPuyoType(nexpuyo.getPuyoType(PuyoNumber.FIRST));
		int nexs = ReadPuyoType(nexpuyo.getPuyoType(PuyoNumber.SECOND));
		int nexnexf = ReadPuyoType(nexnexpuyo.getPuyoType(PuyoNumber.FIRST));
		int nexnexs = ReadPuyoType(nexnexpuyo.getPuyoType(PuyoNumber.SECOND));
		FastBitNextField bnf = new FastBitNextField();
		if (grace < 3 && board.getTotalNumberOfOjama() >= ojamathreshold) {
			/* 3ターン以内（最長でもこのターンの次の次のターンが終わったとき）
			 *  におじゃまが降ってくるとき、おじゃまが降る前に連鎖を発火させる。
			 *  発火できないときはカウンター形を築く。
			 */
			FieldInfo firstfield = bnf.ReadField(board);
			bnf.Calc(firstfield, true, false);
			FieldInfo[] firstfields = firstfield.AvailableFields(nexf, nexs);
			myactionnum = firstfields.length;
			int[] maxsumscores = new int[myactionnum];
			int[][] savedactions = firstfield.availableactions;
			List<Integer> ojamalist = board.getNumbersOfOjamaList();
			if (grace == 1) {
				int ojamanum = ojamalist.get(0);
				int ojamanum2 = ojamalist.get(1);
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i],  false, false);
					int ojamadan = (ojamanum - firstfields[i].score / 70 + 5) / 6;
					bnf.FallDownOjama(firstfields[i], ojamadan);
					bnf.ThinkNextActions(firstfields[i]);
					FieldInfo[] secondfields = firstfields[i].AvailableFields();
					for (int j=0;j<secondfields.length;j++) {
						bnf.Calc(secondfields[j], false, false);
						int ojamadan2 = (ojamanum2 - secondfields[j].score / 70 + 5) / 6;
						bnf.FallDownOjama(secondfields[j], ojamadan2);
						bnf.ThinkNextActions(secondfields[j]);
						FieldInfo[] thirdfields = secondfields[j].AvailableFields();
						for (int k=0;k<thirdfields.length;k++) {
							bnf.Calc(thirdfields[k], false, false);
							int temp = firstfields[i].score + secondfields[j].score + thirdfields[k].score + 2000 - bnf.OjamaDiscount(thirdfields[k]);
							maxsumscores[i] = Math.max(maxsumscores[i], temp);
						}
					}
				}
			}
			else if (grace == 2) {
				int ojamanum = ojamalist.get(1);
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(firstfields[i], true, false);
					FieldInfo[] secondfields = firstfields[i].AvailableFields(nexnexf, nexnexs);
					for (int j=0;j<secondfields.length;j++) {
						bnf.Calc(secondfields[j], false, false);
						// おじゃまを降らせてからのカウンターを考える
						int ojamadan = (ojamanum - (firstfields[i].score + secondfields[j].score) / 70 + 5) / 6;
						bnf.FallDownOjama(secondfields[j], ojamadan);
						bnf.ThinkNextActions(secondfields[j]);
						FieldInfo[] thirdfields = secondfields[j].AvailableFields();
						for (int k=0;k<thirdfields.length;k++) {
							bnf.Calc(thirdfields[k], false, false);
							int temp = firstfields[i].score + secondfields[j].score + thirdfields[k].score + 2000 - bnf.OjamaDiscount(thirdfields[k]);
							maxsumscores[i] = Math.max(maxsumscores[i], temp);
						}
					}
				}
			}
			else if (grace == 3) {
				int ojamanum = ojamalist.get(2);
				FieldInfo myfirstfield = bnf.ReadField(board);
				bnf.Calc(myfirstfield, true, false);
				FieldInfo[] myfirstfields = myfirstfield.AvailableFields(nexf, nexs);
				myactionnum = myfirstfields.length;
				for (int i=0;i<myactionnum;i++) {
					bnf.Calc(myfirstfields[i], true, false);
					FieldInfo[] mysecondfields = myfirstfields[i].AvailableFields(nexnexf, nexnexs);
					int[] maxscoresofeachtsumo = new int[15];
					for (int j=0;j<mysecondfields.length;j++) {
						bnf.Calc(mysecondfields[j], true, false);
						FieldInfo[] mythirdfields = mysecondfields[j].AvailableFields(0, 0);
						for (int k=0;k<mythirdfields.length;k++) {
							bnf.Calc(mythirdfields[k], false, false);
							int tempthirdscore = mythirdfields[k].score;
							int ojamadan = (ojamanum - (myfirstfields[i].score + mysecondfields[j].score + tempthirdscore) / 70 + 5) / 6;
							bnf.CalcPlacetoFire(mythirdfields[k], ojamadan);
							int[] fourthscores = bnf.Tsumos(mythirdfields[k]);	
							int temp = mysecondfields[j].score + tempthirdscore;		
							bnf.CompareScores(maxscoresofeachtsumo, fourthscores, temp);
						}
					}
					int sumscore = 0;
					for (int j=0;j<15;j++) {
						sumscore += maxscoresofeachtsumo[j];
					}
					maxsumscores[i] = myfirstfields[i].score + sumscore / 15;
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
				System.out.println("参りました");
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
			int enemysecondscore = 0;
			for (int i=0;i<enemyactionnum;i++) {
				bnf.Calc(enemyfirstfields[i], true, false);
				FieldInfo[] enemysecondfields = enemyfirstfields[i].AvailableFields(nexnexf, nexnexs);
				int tempfirstscore = enemyfirstfields[i].score;
				enemyfirstscore = Math.max(enemyfirstscore, tempfirstscore);
				int[] maxscoresofeachtsumo = new int[15]; 
				for (int j=0;j<enemysecondfields.length;j++) {
					bnf.Calc(enemysecondfields[j], true, false);
					FieldInfo[] enemythirdfields = enemysecondfields[j].AvailableFields(0, 0);
					int tempsecondscore = enemysecondfields[j].score;
					enemysecondscore = Math.max(enemysecondscore, tempsecondscore);
					for (int k=0;k<enemythirdfields.length;k++) {
						bnf.CalcPlacetoFire(enemythirdfields[k]);
						int tempthirdscore = enemythirdfields[k].score;
						int[] fourthscores = bnf.Tsumos(enemythirdfields[k]);
						int temp = tempsecondscore + tempthirdscore;
						bnf.CompareScores(maxscoresofeachtsumo, fourthscores, temp);
					}
				}
				int sumscore = 0;
				for (int j=0;j<15;j++) {
					sumscore += maxscoresofeachtsumo[j];
				}
				// Arrays.sort(maxscoresofeachtsumo);
				enemymaxsumscores[i] = tempfirstscore + sumscore / 15;
			}
			int enemymaxscore = 0;
			for (int i=0;i<enemyactionnum;i++) {
				if (enemymaxsumscores[i] > enemymaxscore) {
					enemymaxscore = enemymaxsumscores[i];
				}
			}
			
			double[] tempmulti = {0, 0.5, 0.4, 0.25};
			bnf.firepossibilityevaluator = (firepos, numtof) -> firepos * tempmulti[numtof];
			
			// 4手先まで読む
			FieldInfo myfirstfield = bnf.ReadField(board);
			bnf.Calc(myfirstfield, true, false);
			FieldInfo[] myfirstfields = myfirstfield.AvailableFields(nexf, nexs);
			myactionnum = myfirstfields.length;
			double[] mymaxsumscores = new double[myactionnum];
			int[] firstscores = new int[myactionnum];
			int[][] savedactions = myfirstfield.availableactions;
			int[] stablecounters = new int[myactionnum];
			double[] averagecounters = new double[myactionnum];
			int blanknum = CountBlank();
			for (int i=0;i<myactionnum;i++) {
				bnf.Calc(myfirstfields[i], true, false);
				FieldInfo[] mysecondfields = myfirstfields[i].AvailableFields(nexnexf, nexnexs);
				int tempfirstscore = myfirstfields[i].score;
				firstscores[i] =  tempfirstscore;
				int[] maxscoresofeachtsumo = new int[15]; 
				int[] maxscoresofeachtsumost = new int[15]; 
				for (int j=0;j<mysecondfields.length;j++) {
					bnf.Calc(mysecondfields[j], true, false);
					FieldInfo[] mythirdfields = mysecondfields[j].AvailableFields(0, 0);
					int tempsecondscore = mysecondfields[j].score;
					for (int k=0;k<mythirdfields.length;k++) {
						bnf.CalcPlacetoFire(mythirdfields[k]);
						int tempthirdscore = mythirdfields[k].score;
						double thirdpossibility = mythirdfields[k].firepossibility;
						int[] fourthscores = bnf.Tsumos(mythirdfields[k]);		
						int temp = (int)(tempsecondscore * 2.2 + tempthirdscore * 2.1 + 3 * thirdpossibility - 2 * bnf.OjamaDiscount(mythirdfields[k]));
						int tempst = tempsecondscore + tempthirdscore;
						bnf.CompareScores(maxscoresofeachtsumo, fourthscores, temp);
						bnf.CompareScores(maxscoresofeachtsumost, fourthscores, tempst);
						stablecounters[i] = Math.max(stablecounters[i], tempfirstscore + tempst);
					}
				}
				for (int j=0;j<15;j++) {
					averagecounters[i] += maxscoresofeachtsumost[j];
				}
				averagecounters[i] = tempfirstscore + averagecounters[i] / 15;
				if (blanknum > 40) {
					Arrays.sort(maxscoresofeachtsumo);
					mymaxsumscores[i] = tempfirstscore * 2.3 + maxscoresofeachtsumo[14];
				}
				else {
					for (int j=0;j<15;j++) {
						mymaxsumscores[i] += maxscoresofeachtsumo[j];
					}
					mymaxsumscores[i] = tempfirstscore * 2.3 + mymaxsumscores[i] / 15;
				}
			}
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
			

			long end = System.currentTimeMillis();
			System.out.println(end - start);
			System.out.println("--------------------------------");

			if (myactionnum == 0) {
				System.out.println("参りました");
				return new Action(PuyoDirection.DOWN, 0);
			}
			else if (firstmaxscore > enemymaxscore + 1000 && board.getTotalNumberOfOjama() < ojamathreshold) {
				// 相手の組んでいる連鎖が小さくて速攻で倒せそうだったら
				System.out.println("速攻を仕掛けます");
				PuyoDirection selectdirection = PuyoDirection.values()[savedactions[firstmaxindex][0]];
				int selectcolumn = savedactions[firstmaxindex][1];
				return  new Action(selectdirection, selectcolumn);
			}
			else if (getEnemyBoard().getTotalNumberOfOjama() < 10 && enemyfirstscore > 1000) {
				// このターンで相手に1000点以上発火される可能性があるとき
				if (stablecounters[selectindex] > enemyfirstscore) {
					// 確実に対処できるとき
					// selectindexはそのままにしておく
				}
				else if (averagecounters[selectindex] + 1000 > enemyfirstscore) {
					// おそらく対処できるとき
					// この場合もselectindexはそのままにしておく
				}
				else {
					// このままでは対処できなさそうなとき
					int stablemax = 0;
					double averagemax = 0;
					int stablemaxindex = 0;
					int averagemaxindex = 0;
					for (int i=0;i<myactionnum;i++) {
						if (stablecounters[i] > stablemax) {
							stablemax = stablecounters[i];
							stablemaxindex = i;
						}
						if (averagecounters[i] > averagemax) {
							averagemax = averagecounters[i];
							averagemaxindex = i;
						}
					}
					if (stablemax > enemyfirstscore) {
						// stablemaxindexに変えれば対処できるとき
						System.out.println("相手の発火を危惧して安全策を取りました");
						PuyoDirection selectdirection = PuyoDirection.values()[savedactions[stablemaxindex][0]];
						int selectcolumn = savedactions[stablemaxindex][1];
						return  new Action(selectdirection, selectcolumn);
					}
					else if (averagemax + 1000 > enemyfirstscore) {
						System.out.println("相手の発火を危惧して比較的安全な策を取りました");
						PuyoDirection selectdirection = PuyoDirection.values()[savedactions[averagemaxindex][0]];
						int selectcolumn = savedactions[averagemaxindex][1];
						return  new Action(selectdirection, selectcolumn);
					}
					else {
						System.out.println("今発火されたらきついです…");
					}
				}
			}
			if (myaction == null) {
				if (firstscores[selectindex] > 0 && CountBlank() > 20) {
					// 発火させる決断をする直前
					// 終盤は発火抑制をoffにする
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
					if (maxscore < scorethreshold) {
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
