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

public class SimpleNextField {
	public int[] fieldtops;
	public int[][] field;
	public List<Integer> myojama;
	public List<Integer> enemyojama;
	public List<Integer> nextmyojama;
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
		fieldtops = new int[width];
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
		if (mypuyo.getDirection() == Puyo.PuyoDirection.DOWN) {
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
	
	public int[][] FieldtoFieldMatrix(Field field){
		int[][] output = new int[field.getWidth()][field.getHeight()-1];
		for (int i=0;i<field.getWidth();i++) {
			for (int j=0;j<field.getHeight()-1;j++) {
				output[i][j] = PTtoInt(field.getPuyoType(i, j));
			}
		}
		return output;
	}
	
	public List<Integer> CalcOjama() {
		// おじゃまを降らせる
		int myojamasum = 0;
		for (Integer myo : myojama) {
			myojamasum += myo;
		}
		int tempojama = myscore / 70;
		nextmyscore = myscore % 70;
		List<Integer> tempmyojama = new ArrayList<Integer>();
		for (int myo : myojama) {
			tempmyojama.add(myo);
		}
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
						tempojama -= tempmyojama.get(tempojamaturn);
						tempmyojama.set(tempojamaturn, 0);
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
				for (int i=0;i<3;i++) {
					tempmyojama.add(0);
				}
				tempojama -= myojamasum;
				while (tempojama > 0) {
					if (tempojama >= 30) {
						tempmyojama.add(-30);
						tempojama -= 30;
					}
					else {
						tempmyojama.add(-tempojama);
						tempojama = 0;
					}
				}
			}
		}
		return tempmyojama;
	}
	
	public boolean IsAlive(int[][] field) {
		boolean isdead = false;
		for (int i=0;i<field.length;i++) {
			for (int j=12;j<field[i].length;j++) {
				if (field[i][j] > 0) {
					isdead = true;
					break;
				}
			}
			if (isdead) {
				break;
			}
		}
		return !isdead;
	}
	
	public int[][] RainDownOjama(List<Integer> myojamalist) {
		int thisturnojama = myojamalist.get(0);
		myojamalist.remove(0);
		nextmyojama = myojamalist;
		int raindan = thisturnojama / 6;
		int randomojama = thisturnojama % 6; // １段に満たない分はランダムに降る
		// 6Crandomojama 通りがあるのでそれらすべてを列挙する
		int[][] tempfield = Arrays.copyOf(field, width);
		// まず必要な分のおじゃまを降らせる
		for (int i=0;i<width;i++) {
			for (int j=fieldtops[i]+1;j<Math.min(fieldtops[i]+raindan+1, height-1); j++) {
				tempfield[i][j] = 6;
			}
			fieldtops[i]++;
		}
		if (randomojama > 0) {
			int aiterunum = 0;
			for (int j=0;j<width;j++) {
				if ( fieldtops[j]<height-2) {
					aiterunum++;
				}
			}
			for (int j=0;j<width;j++) {
				if ( fieldtops[j]<height-2) {
					tempfield[j][fieldtops[j]+1] = 6 + aiterunum;
				}
			}
		}
		return tempfield;
	}
	
	public boolean SearchField(int nrensa) {
		List<List<int[]>> rensapuyo = new ArrayList<List<int[]>>();
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (field[i][j] != 0 && field[i][j] != 6) {
					// おじゃまぷよだったりそもそもぷよがない場合を除く
					// 各ぷよについてすでに調べたぷよとつながっているかを見る
					int kuminum = rensapuyo.size();
					boolean[] connect = new boolean[kuminum];
					int tempkuminum = 0;
					for (List<int[]> kumi : rensapuyo) {
						for (int[] puyo : kumi) {
							boolean isneighbor = ((puyo[0] == i) &&  (puyo[1] == j + 1 || puyo[1] == j - 1)) || ((puyo[0] == i + 1 || puyo[0] == i - 1) && (puyo[1] == j));
							if (isneighbor && (puyo[2] == field[i][j])) {
								connect[tempkuminum] = true;
								break;
							}
						}
						tempkuminum++;
					}
					
					// 既に調べたぷよと一つでもつながっているかを見る
					int connectnum = 0;
					for (int k=0;k<kuminum;k++) {
						if (connect[k]) {
							connectnum++;
						}
					}

					List<int[]> tempkumi = new ArrayList<int[]>();
					int[] temppuyoinfo = {i, j, field[i][j]};
					tempkumi.add(temppuyoinfo);
					
					if (connectnum == 0) {
						// 既に調べたぷよと一つもつながっていなかったら
						rensapuyo.add(tempkumi);
					}
					else {
						for (int k=kuminum-1;k>=0;k--) {
							if (connect[k]) {
								tempkumi.addAll(rensapuyo.get(k));
								rensapuyo.remove(k); // 後ろから取り去っていけば前のインデックスは変化しない
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
						if (j>0) {
							temperase = temperase || eraseplace[i][j-1];
						}
						if (j<height-1) {
							temperase = temperase || eraseplace[i][j+1];
						}
						if (i>0) {
							temperase = temperase || eraseplace[i-1][j];
						}
						if (i<width-1) {
							temperase = temperase || eraseplace[i+1][j];
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
						for (int k=j;k<height-1;k++) {
							field[i][k] = field[i][k+1];
						}
						field[i][height-1] = 0;
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
