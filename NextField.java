package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class NextField {
	public int[] fieldtops;
	public int[][] field;
	public int[][] nextfield;
	public int[][][] nextfieldwithojama;
	public List<Integer> myojama;
	public List<Integer> enemyojama;
	public List<Integer> nextmyojama;
	public List<Integer> nextenemyojama;
	public int myscore;
	public int nextmyscore;
	public Board myboard;
	public Field myfield;
	public int width;
	public int height;
	public int colornum;
	
	public void Settings(Puyo mypuyo, int firstx, Board board, PlayerInfo me, Board enemyboard) {
		myboard = board;
		myfield = board.getField();
		width = myfield.getWidth();
		height = myfield.getHeight()-1;
		colornum = 5;
		myscore = me.getOjamaScore();
		myojama = myboard.getNumbersOfOjamaList();
		enemyojama = enemyboard.getNumbersOfOjamaList();
		field = new int[width][height];
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				field[i][j] = PTtoInt(myfield.getPuyoType(i, j));
				if (field[i][j] == 0) {
					fieldtops[i] = j-1;
					break;
				}
			}
		}
		int firstpuyo = PTtoInt(mypuyo.getPuyoType(PuyoNumber.FIRST));
		int secondpuyo = PTtoInt(mypuyo.getPuyoType(PuyoNumber.SECOND));
		int secondx = firstx + mypuyo.getSecondColmNumber();
		// まずぷよを置く
		if (mypuyo.getDirection() == Puyo.PuyoDirection.UP) {
			// firstpuyoが上の状態で置くときはsecondpuyoを先に置く
			field[secondx][fieldtops[secondx]+1] = secondpuyo;
			fieldtops[secondx]++;
			field[firstx][fieldtops[firstx]+1] = firstpuyo;
			fieldtops[firstx]++;
		}
		else {
			field[firstx][fieldtops[firstx]+1] = firstpuyo;
			fieldtops[firstx]++;
			field[secondx][fieldtops[secondx]+1] = secondpuyo;
			fieldtops[secondx]++;
		}
		
		// 置いたぷよが連鎖するかを見る
		int nrensa = 1;
		boolean rensacontinues = true;
		while (rensacontinues) {
			if (SearchField(nrensa)) {
				nrensa++;
			}
			else {
				nrensa--;
				rensacontinues = false;
			}
		}
	}
	
	
	
	public List<Integer>[] CalcOjama(int sprungenemyojama) {
		// おじゃまを降らせる
		int myojamasum = 0;
		int enemyojamasum = 0;
		for (Integer myo : myojama) {
			myojamasum += myo;
		}
		for (Integer eno : enemyojama) {
			enemyojamasum += eno;
		}
		int tempojama = myscore / 70 - sprungenemyojama;
		nextmyscore = myscore % 70;
		List<Integer> tempmyojama = new ArrayList<Integer>();
		List<Integer> tempenemyojama = new ArrayList<Integer>();
		for (int myo : myojama) {
			tempmyojama.add(myo);
		}
		for (int eno : enemyojama) {
			tempenemyojama.add(eno);
		}
		if (myojamasum > 0) {
			// おじゃまぷよが自分の方に降ってくる場合
			if (tempojama > 0) {
				// 相殺できる場合
				if (myojamasum > tempojama) {
					// すべては相殺できない場合
					int tempojamaturn = 0;
					while (tempojama > 0) {
						if (tempmyojama.size() <= tempojamaturn) {
							for (int i=tempmyojama.size();i<=tempojamaturn;i++) {
								tempmyojama.add(0);
							}
						}
						if (tempojama >= tempmyojama.get(tempojamaturn)) {
							tempmyojama.set(tempojamaturn, 0);
							tempojama -= tempmyojama.get(tempojamaturn);
						}
						else {
							tempmyojama.set(tempojamaturn, tempmyojama.get(tempojamaturn) - tempojama);
							tempojama = 0;
						}
						tempojamaturn++;
					}
				}
				else {
					// すべて相殺できる場合
					tempmyojama = new ArrayList<Integer>();
					for (int i=0;i<4;i++) {
						tempmyojama.add(0);
					}
					tempojama -= myojamasum;
					if (tempenemyojama.size() < tempojama/30+4) {
						for (int i=tempenemyojama.size();i<tempojama/30+4;i++) {
							tempenemyojama.add(0);
						}
					}
					for (int i=0;i<tempojama/30;i++) {
						tempenemyojama.set(i+3, 30);
					}
					tempenemyojama.set(tempojama/30+3, tempojama%30);
				}
			}
			else {
				// 自分の方に降ってくるおじゃまぷよがさらに増やされる場合
				tempojama *= -1;
				int tempojamaturn = 3;
				while (tempojama > 0) {
					if (tempmyojama.size() <= tempojamaturn) {
						for (int i=tempmyojama.size();i<=tempojamaturn;i++) {
							tempmyojama.add(0);
						}
					}
					if (tempojama > (30 - tempmyojama.get(tempojamaturn))) {
						tempojama -= 30 - tempmyojama.get(tempojamaturn);
						tempmyojama.set(tempojamaturn, 30);
					}
					else {
						tempmyojama.set(tempojamaturn, tempmyojama.get(tempojamaturn) + tempojama);
						tempojama = 0;
					}
					tempojamaturn++;
				}
			}
		}
		else if (enemyojamasum > 0) {
			// おじゃまぷよが相手の方に降ってくる場合
			tempojama *= -1;
			if (tempojama > 0) {
				// 相手が相殺できる場合
				if (enemyojamasum > tempojama) {
					// すべては相殺できない場合
					int tempojamaturn = 0;
					while (tempojama > 0) {
						if (tempenemyojama.size() <= tempojamaturn) {
							for (int i=tempenemyojama.size();i<=tempojamaturn;i++) {
								tempenemyojama.add(0);
							}
						}
						if (tempojama >= tempenemyojama.get(tempojamaturn)) {
							tempenemyojama.set(tempojamaturn, 0);
							tempojama -= tempenemyojama.get(tempojamaturn);
						}
						else {
							tempenemyojama.set(tempojamaturn, tempenemyojama.get(tempojamaturn) - tempojama);
							tempojama = 0;
						}
						tempojamaturn++;
					}
				}
				else {
					// すべて相殺できる場合
					tempenemyojama = new ArrayList<Integer>();
					for (int i=0;i<4;i++) {
						tempenemyojama.add(0);
					}
					if (tempmyojama.size() < tempojama/30+4) {
						for (int i=tempmyojama.size();i<tempojama/30+4;i++) {
							tempmyojama.add(0);
						}
					}
					tempojama -= enemyojamasum;
					for (int i=0;i<tempojama/30;i++) {
						tempmyojama.set(i+3, 30);
					}
					tempmyojama.set(tempojama/30+3, tempojama%30);
				}
			}
			else {
				// 相手の方に降ってくるおじゃまぷよがさらに増やされる場合
				tempojama *= -1;
				int tempojamaturn = 3;
				while (tempojama > 0) {
					if (tempenemyojama.size() <= tempojamaturn) {
						for (int i=tempenemyojama.size();i<=tempojamaturn;i++) {
							tempenemyojama.add(0);
						}
					}
					if (tempojama > (30 - tempenemyojama.get(tempojamaturn))) {
						tempojama -= 30 - tempenemyojama.get(tempojamaturn);
						tempenemyojama.set(tempojamaturn, 30);
					}
					else {
						tempenemyojama.set(tempojamaturn, tempenemyojama.get(tempojamaturn) + tempojama);
						tempojama = 0;
					}
					tempojamaturn++;
				}
			}
		}
		List<Integer>[] output = new ArrayList[2];
		output[0] = tempmyojama;
		output[1] = tempenemyojama;
		return output;
	}
	
	public int[][][] RainDownOjama(List<Integer> myojamalist) {
		int thisturnojama = myojamalist.get(0);
		myojamalist.remove(0);
		nextmyojama = myojamalist;
		int raindan = thisturnojama / 6;
		int randomojama = thisturnojama % 6; // １段に満たない分はランダムに降る
		// 6Crandomojama 通りがあるのでそれらすべてを列挙する
		boolean[][] rainpatterns = SixCnumber(randomojama);
		nextfieldwithojama = new int[rainpatterns.length][width][height];
		int[][] tempfield = Arrays.copyOf(field, width * height);
		// まず必要な分のおじゃまを降らせる
		for (int i=0;i<width;i++) {
			for (int j=fieldtops[i]+1;j<Math.min(fieldtops[i]+raindan+1, height); j++) {
				tempfield[i][j] = 6;
			}
			fieldtops[i]++;
		}
		for (int i=0;i<rainpatterns.length;i++) {
			nextfieldwithojama[i] = Arrays.copyOf(tempfield, width * height);
			for (int j=0;j<width;j++) {
				if (rainpatterns[i][j]) {
					nextfieldwithojama[i][j][fieldtops[j]+1] = 6;
					fieldtops[j]++;
				}
			}
		}
		return nextfieldwithojama;
	}
	
	public boolean[][] SixCnumber(int number) {
		switch (number) {
		case 0:
			boolean[][] output0 = {{false, false, false, false, false, false}};
			return output0;
		case 1:
			boolean[][] output1 = {{true, false, false, false, false, false},
					{false, true, false, false, false, false},
					{false, false, true, false, false, false},
					{false, false, false, true, false, false},
					{false, false, false, false, true, false},
					{false, false, false, false, false, true}};
			return output1;
		case 2:
			boolean[][] output2 = {{true, true, false, false, false, false},
					{true, false, true, false, false, false},
					{true, false, false, true, false, false},
					{true, false, false, false, true, false},
					{true, false, false, false, false, true},
					{false, true, true, false, false, false},
					{false, true, false, true, false, false},
					{false, true, false, false, true, false},
					{false, true, false, false, false, true},
					{false, false, true, true, false, false},
					{false, false, true, false, true, false},
					{false, false, true, false, false, true},
					{false, false, false, true, true, false},
					{false, false, false, true, false, true},
					{false, false, false, false, true, true}};
			return output2;
		case 3:
			boolean[][] output3 = {{true, true, true, false, false, false},
					{true, true, false, true, false, false},
					{true, true, false, false, true, false},
					{true, true, false, false, false, true},
					{true, false, true, true, false, false},
					{true, false, true, false, true, false},
					{true, false, true, false, false, true},
					{true, false, false, true, true, false},
					{true, false, false, true, false, true},
					{true, false, false, false, true, true},
					{false, true, true, true, false, false},
					{false, true, true, false, true, false},
					{false, true, true, false, false, true},
					{false, true, false, true, true, false},
					{false, true, false, true, false, true},
					{false, true, false, false, true, true},
					{false, false, true, true, true, false},
					{false, false, true, true, false, true},
					{false, false, true, false, true, true},
					{false, false, false, true, true, true}};
			return output3;
		case 4:
			boolean[][] output2_ = {{true, true, false, false, false, false},
					{true, false, true, false, false, false},
					{true, false, false, true, false, false},
					{true, false, false, false, true, false},
					{true, false, false, false, false, true},
					{false, true, true, false, false, false},
					{false, true, false, true, false, false},
					{false, true, false, false, true, false},
					{false, true, false, false, false, true},
					{false, false, true, true, false, false},
					{false, false, true, false, true, false},
					{false, false, true, false, false, true},
					{false, false, false, true, true, false},
					{false, false, false, true, false, true},
					{false, false, false, false, true, true}};
			boolean[][] output4 = new boolean[15][6];
			for (int i=0;i<15;i++) {
				for (int j=0;j<6;j++) {
					output4[i][j] = !output2_[i][j];
				}
			}
			return output4;
		case 5:
			boolean[][] output1_ = {{true, false, false, false, false, false},
					{false, true, false, false, false, false},
					{false, false, true, false, false, false},
					{false, false, false, true, false, false},
					{false, false, false, false, true, false},
					{false, false, false, false, false, true}};
			boolean[][] output5 = new boolean[6][6];
			for (int i=0;i<6;i++) {
				for (int j=0;j<6;j++) {
					output5[i][j] = !output1_[i][j];
				}
			}
			return output5;
		case 6:
			boolean[][] output6 = {{true, true, true, true, true, true}};
			return output6;
		default:
			boolean[][] output0_ = {{false, false, false, false, false, false}};
			return output0_;
		}
	}
	
	
	public boolean SearchField(int nrensa) {
		List<List<int[]>> rensapuyo = new ArrayList<List<int[]>>();
		int kuminum = 0;
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (field[i][j] != 0 && field[i][j] != 6) {
					// おじゃまぷよだったりそもそもぷよがない場合を除く
					// 各ぷよについてすでに調べたぷよとつながっているかを見る
					boolean[] connect = new boolean[kuminum];
					// eclipseのデフォルトだとソースレベルが低すぎるかもしれない
					int tempkuminum = 0;
					for (List<int[]> kumi : rensapuyo) {
						for (int[] puyo : kumi) {
							int tempdistance = (puyo[0] - i) * (puyo[1] - j);
							connect[tempkuminum] = (tempdistance == 1 || tempdistance == -1) && (puyo[2] == field[i][j]); 
						}
						tempkuminum++;
					}
					
					// 既に調べたぷよと一つでもつながっているかを見る
					boolean isconnected = false;
					for (int k=0;k<kuminum;k++) {
						if (connect[k]) {
							isconnected = true;
							break;
						}
					}

					List<int[]> tempkumi = new ArrayList<int[]>();
					int[] temppuyoinfo = {i, j, field[i][j]};
					tempkumi.add(temppuyoinfo);
					
					if (!isconnected) {
						// 既に調べたぷよと一つもつながっていなかったら
						rensapuyo.add(tempkumi);
					}
					else {
						for (int k=tempkuminum-1;k>=0;k--) {
							if (connect[k]) {
								tempkumi.addAll(rensapuyo.get(k));
								tempkumi.remove(k); // 後ろから取り去っていけば前のインデックスは変化しない
							}
						}
						rensapuyo.add(tempkumi);
					}
				}
			}
		}
		
		// おじゃまが消えるかを見る
		boolean[] erasecolor = new boolean[colornum];
		List<Integer> erasepuyonums = new ArrayList<Integer>();
		boolean[][] eraseplace = new boolean[width][height]; // 消えるぷよの場所　これを利用しておじゃまが消えるか見る
		for (List<int[]> kumi : rensapuyo) {
			if (kumi.size() > 3) {
				for (int[] puyo : kumi) {
					eraseplace[puyo[0]][puyo[1]] = true;
				}
				erasecolor[kumi.get(0)[2] - 1] = true;
				erasepuyonums.add(kumi.size());
			}
		}
		int erasecolornum = 0;
		for (int i=0;i<colornum;i++) {
			if (erasecolor[i]) {
				erasecolornum++;
			}
		}
		if (erasepuyonums.size() == 0) {
			// 連鎖が起きなかったら
			return false;
		}
		else {
			for (int i=0;i<width;i++) {
				for (int j=0;j<height;j++) {
					if (field[i][j] == 6) {
						boolean temperase = false;
						if (i>0) {
							if (j>0) {
								temperase = temperase || eraseplace[i-1][j-1];
							}
							if (j<height-1) {
								temperase = temperase || eraseplace[i-1][j+1];
							}
						}
						if (i<width-1) {
							if (j>0) {
								temperase = temperase || eraseplace[i+1][j-1];
							}
							if (j<height-1) {
								temperase = temperase || eraseplace[i+1][j+1];
							}
						}
						eraseplace[i][j] = temperase;
					}
				}
			}
			
			// 得点の計算
			myscore += Tokuten(nrensa, erasepuyonums, erasecolornum);
			
			// 次の状態を見る
			for (int i=0;i<width;i++) {
				for (int j=height-1;j>=0;j--) {
					if (eraseplace[i][j]) {
						for (int k=j;k<height;k++) {
							field[i][k] = field[i][k+1];
						}
						fieldtops[i]--;
					}
				}
				for (int j=fieldtops[i]+1;j<height;j++) {
					field[i][j] = 0;
				}
			}
			
			// 全消しかを見る
			boolean iszenkeshi = true;
			for (int i=0;i<width;i++) {
				for (int j=0;j<height;j++) {
					if (field[i][j] != 0) {
						iszenkeshi = false;
						break;
					}
				}
				if (!iszenkeshi) {
					break;
				}
			}
			if (iszenkeshi) {
				myscore += 2100;
			}
			return true;
		}
	}
	
	public int Tokuten(int rensa, List<Integer> erasepuyonums, int erasecolornum) {
		int kihonten = 0;
		int bairitsu = 0;
		if (erasecolornum > 1) {
			bairitsu = 3;
			for (int i=2;i<erasecolornum;i++) {
				bairitsu *= 2;
			}
		}
		for (int puyonum : erasepuyonums) {
			kihonten += puyonum * 10;
			if (puyonum > 4 && puyonum < 11) {
				bairitsu += puyonum - 3;
			}
			else if (puyonum >= 11) {
				bairitsu += 10;
			}
		}
		if (rensa > 1 && rensa < 6) {
			// 2~5連鎖のとき
			int tempbairitsu = 2;
			for (int i=0;i<rensa;i++) {
				tempbairitsu *= 2;
			}
			bairitsu += tempbairitsu;
		}
		else if (rensa >= 6) {
			bairitsu += 32 * (rensa - 3);
		}
		if (bairitsu < 1) {
			bairitsu = 1;
		}
		int tokuten = kihonten * bairitsu;
		return tokuten;
	}
	
	public int getPuyo(int x, int y) {
		if (x < 0 || x > width-1 || y < 0 || y > height-1) {
			return -1;
		}
		else {
			return field[x][y];
		}
	}
	
	public int PTtoInt(PuyoType puyotype) {
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
			case OJAMA_PUYO:
				return 6;
			default:
				return 0;
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
}
