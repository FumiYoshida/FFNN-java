package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class FirePossibility {
	// 次にどんな色が来てほしいかを求める
	public int[] colorstofire;
	public int numtofire;
	public String request;
	public int[][] nextfield;
	public int myscoreincrement;
	public int width = 6;
	public int safeheight = 12;
	public int height = 14;
	public int colornum = 5;
	public int tempnum = 0;
	
	public Puyo[] availablemypuyos;
	public int[] availablemypuyocolumns;
	public Puyo[] submypuyos; // 置いた直後に連鎖が起こって12段目までに落とされない限り死ぬ手
	public int[] submypuyocolumns;
	
	public List<List<int[]>> DeepClone(List<List<int[]>> input){
		List<List<int[]>> output = new ArrayList<List<int[]>>();
		for (int i=0;i<input.size();i++) {
			List<int[]> temp = new ArrayList<int[]>();
			for (int j=0;j<input.get(i).size();j++) {
				temp.add(Arrays.copyOf(input.get(i).get(j), input.get(i).get(j).length));
			}
			output.add(temp);
		}
		return output;
	}
	
	public int[][] DeepClone(int[][] input){
		int[][] output = new int[input.length][];
		for (int i=0;i<input.length;i++) {
			output[i] = Arrays.copyOf(input[i], input[i].length);
		}
		return output;
	}
	
	public int Calc(int[][] currentfield) {
		int[][] tempfield = new int[width][height];
		int[] fieldtops = new int[width];
		for (int i=0;i<width;i++) {
			fieldtops[i] = safeheight-1;
			for (int j=0;j<safeheight;j++) {
				tempfield[i][j] = currentfield[i][j];
				if (currentfield[i][j] == 0) {
					fieldtops[i] = j-1;
					break;
				}
			}
		}
		List<List<int[]>> cpuyos = ConnectingPuyos(tempfield);
		List<List<List<int[]>>> tempcpuyos = new ArrayList<List<List<int[]>>>();
		boolean[] isedge = new boolean[cpuyos.size()];
		for (int i=0;i<cpuyos.size();i++) {
			isedge[i] = false;
			for (int j=0;j<cpuyos.get(i).size();j++) {
				int tempx = cpuyos.get(i).get(j)[0];
				int tempy = cpuyos.get(i).get(j)[1];
				if (fieldtops[tempx] == tempy) {
					isedge[i] = true;
				}
				else if (tempx > 0) {
					if (fieldtops[tempx-1] < tempy) {
						isedge[i] = true;
					}
				}
				if (tempx < width-1) {
					if (fieldtops[tempx+1] < tempy) {
						isedge[i] = true;
					}
				}
				if (isedge[i]) {
					break;
				}
			}
			if (isedge[i]) {
				List<List<int[]>> copyofcpuyo = DeepClone(cpuyos);
				List<int[]> temp = copyofcpuyo.get(i);
				int temps = temp.size(); // 途中でtempを書き換えるので先に値を保存しておく
				for (int j=0;j<4-temps;j++) {
					temp.add(temp.get(0)); // copyoucpuyoのgetだけどこれでcopyofcpuyoが書き換わっている
				}
				tempcpuyos.add(copyofcpuyo);
			}
			else {
				tempcpuyos.add(new ArrayList<List<int[]>>());
			}
		}
		int[] scores = new int[cpuyos.size()];
		for (int i=0;i<cpuyos.size();i++) {
			// 連鎖するかを見る
			if (isedge[i]) {
				int nrensa = 1;
				boolean rensacontinues = true;
				int[] tempfieldtops = Arrays.copyOf(fieldtops, fieldtops.length);
				int[][] tempfieldclone = DeepClone(tempfield);
				while (rensacontinues) {
					if (nrensa == 1) {
						if (SearchField(nrensa, tempfieldclone, tempfieldtops, tempcpuyos.get(i), scores, i)) {
							nrensa++;
						}
						else {
							nrensa--;
							rensacontinues = false;
						}
					}
					else {
						if (SearchField(nrensa, tempfieldclone, tempfieldtops, scores, i)) {
							nrensa++;
						}
						else {
							nrensa--;
							rensacontinues = false;
						}
					}
				}
			}
		}
		int maxscore = 0;
		int maxscoreindex = 0;
		for (int i=0;i<cpuyos.size();i++) {
			if (isedge[i] && scores[i] > maxscore) {
				maxscore = scores[i];
				maxscoreindex = i;
			}
		}
		// この時点で、どれか一つのぷよの塊を消したときに、最も得点が高くなるものが求まった。
		// 表層のを 一つ消せば大連鎖が始まる場合は良いが、
		// 二つ以上消さないと大連鎖が始まらない場合や、
		// 奥のを消さないといけない場合について考えなければならない。
		
		// でもまあ一旦これで出力する
		// ほしいぷよを考える
		colorstofire = new int[5];
		numtofire = 0;
		if (cpuyos.size() > 0) {
			List<int[]> ignitionpoint = cpuyos.get(maxscoreindex);
			int ignitioncolor = ignitionpoint.get(0)[2];
			int gunpowderamount = 4 - ignitionpoint.size();
			colorstofire[ignitioncolor - 1] = gunpowderamount;
			numtofire += gunpowderamount;
		}
		MakeRequestString();
		if (maxscore == 40) {
			int a = 0;
			int b = a + 1;
		}
		return maxscore;
	}
	
	public int Calc(Field currentfield) {
		int[][] tempcurf = FieldtoFieldMatrix(currentfield);
		return Calc(tempcurf);
	}
	
	public int[][] FieldtoFieldMatrix(Field field){
		int[][] output = new int[field.getWidth()][field.getHeight()-2];
		for (int i=0;i<field.getWidth();i++) {
			for (int j=0;j<field.getHeight()-2;j++) {
				output[i][j] = PTtoInt(field.getPuyoType(i, j));
			}
		}
		return output;
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
	
	public PuyoType IntToPT(int puyotype) {
		if (puyotype == 0) {
			return null;
		}
		else {
			switch (puyotype) {
			case 1:
				return PuyoType.BLUE_PUYO;
			case 2:
				return PuyoType.GREEN_PUYO;
			case 3:
				return PuyoType.PURPLE_PUYO;
			case 4:
				return PuyoType.RED_PUYO;
			case 5:
				return PuyoType.YELLOW_PUYO;
			case 6:
				return PuyoType.OJAMA_PUYO;
			default:
				return null;
			}
		}
	}
	
	public void MakeRequestString() {
		request = "";
		for (int i=0;i<colorstofire.length;i++) {
			if (colorstofire[i] > 0) {
				request += "need " + colorstofire[i] + " " + IntToPT(i+1).toString() + "\r\n";
			}
		}
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
	
	public List<List<int[]>> ConnectingPuyos(int[][] field){
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
		return rensapuyo;
	}
	
	public boolean SearchField(int nrensa, int[][] field, int[] fieldtops, List<List<int[]>> rensapuyo, int[] scores, int scoreindex) {
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
			scores[scoreindex] += Tokuten(nrensa, erasepuyonums, erasecolornum);
			
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
				scores[scoreindex] += 2100;
			}
			return true;
		}
	}
	
	public boolean SearchField(int nrensa, int[][] field, int[] fieldtops, int[] scores, int scoreindex) {
		List<List<int[]>> rensapuyo = ConnectingPuyos(field);
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
			scores[scoreindex] += Tokuten(nrensa, erasepuyonums, erasecolornum);
			
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
				scores[scoreindex] += 2100;
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
	
	public void MakeAllActions(int[] fieldtops, Puyo puyo) {
		// 自分の行動22通り（次のぷよが2つ同じ色の時は11通り）を列挙する
		boolean[] myactions = new boolean[11];
		for (int i=0;i<width;i++) {
			// 縦に設置できるか
			myactions[i] = fieldtops[i] < 10;
		}
		for (int i=0;i<width-1;i++) {
			// 横に設置できるか
			myactions[i+width] = fieldtops[i] < height-3 && fieldtops[i+1] < height-3;
		}
		int myactionnum = 0;
		for (int i=0;i<width*2-1;i++) {
			if (myactions[i]) {
				myactionnum++;
			}
		}
		PuyoType firstpuyo = puyo.getPuyoType(PuyoNumber.FIRST);
		PuyoType secondpuyo = puyo.getPuyoType(PuyoNumber.SECOND);
		Puyo[] puyos = MakePuyos(puyo);
		int myactionserial = 0;
		int subserial = 0;
		if (firstpuyo == secondpuyo) {
			availablemypuyos = new Puyo[myactionnum];
			availablemypuyocolumns = new int[myactionnum];
			submypuyos = new Puyo[width * 2 - 1 - myactionnum];
			submypuyocolumns = new int[width * 2 - 1 - myactionnum];
			for (int i=0;i<width;i++) {
				if (myactions[i]) {
					availablemypuyos[myactionserial] = puyos[0];
					availablemypuyocolumns[myactionserial] = i;
					myactionserial++;
				}
				else {
					submypuyos[subserial] = puyos[0];
					submypuyocolumns[subserial] = i;
					subserial++;
				}
			}
			for (int i=1;i<width;i++) {
				if (myactions[i+width-1]) {
					availablemypuyos[myactionserial] = puyos[3];
					availablemypuyocolumns[myactionserial] = i;
					myactionserial++;
				}
				else {
					submypuyos[subserial] = puyos[3];
					submypuyocolumns[subserial] = i;
					subserial++;
				}
			}
		}
		else {
			// 設置するぷよの向きが2通り
			availablemypuyos = new Puyo[myactionnum*2];
			availablemypuyocolumns = new int[myactionnum*2];
			submypuyos = new Puyo[(width * 2 - 1 - myactionnum)*2];
			submypuyocolumns = new int[(width * 2 - 1 - myactionnum)*2];
			for (int i=0;i<width;i++) {
				if (myactions[i]) {
					availablemypuyos[myactionserial] = puyos[0];
					availablemypuyos[myactionserial+1] = puyos[2];
					availablemypuyocolumns[myactionserial] = i;
					availablemypuyocolumns[myactionserial+1] = i;
					myactionserial += 2;
				}
				else {
					submypuyos[subserial] = puyos[0];
					submypuyos[subserial+1] = puyos[2];
					submypuyocolumns[subserial] = i;
					submypuyocolumns[subserial+1] = i;
					subserial += 2;
				}
			}
			for (int i=1;i<width;i++) {
				if (myactions[i+width-1]) {
					availablemypuyos[myactionserial] = puyos[1]; // RIGHT
					availablemypuyos[myactionserial+1] = puyos[3]; // LEFT
					availablemypuyocolumns[myactionserial] = i-1; // 0 ~ 4
					availablemypuyocolumns[myactionserial+1] = i; // 1 ~ 5
					myactionserial += 2;
				}
				else {
					submypuyos[subserial] = puyos[1]; // RIGHT
					submypuyos[subserial+1] = puyos[3]; // LEFT
					submypuyocolumns[subserial] = i-1; // 0 ~ 4
					submypuyocolumns[subserial+1] = i; // 1 ~ 5
					subserial += 2;
				}
			}
		}
	}
	
	public void MakeAllActions(Board board) {
		Field f = board.getField();
		int[] fieldtops = new int[width];
		for (int i=0;i<width;i++) {
			for (int j=0;j<height;j++) {
				if (f.getPuyoType(i, j) == null) {
					fieldtops[i] = j-1;
					break;
				}
			}
		}
		MakeAllActions(fieldtops, board.getCurrentPuyo());
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
