package player;

import java.util.Arrays;
import java.util.function.BiFunction;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class FastBitNextField {
	// ビット演算で次のフィールドを考えるクラス
	public BiFunction<Integer, Integer, Double> firepossibilityevaluator;
	public double firepossibilityevaluation;
	public int[][][] scoretable;
	public int[] topindextable;
	public int[][][][][][][][][] availableactionstable; // 大きさ(3*3*3*3*3*3*(0~11)*2, 3*3*3*3*3*3*(0~22)*2) = 48114(以下) (puyoをrotateさせる回数、置く場所)の組
	public int[][][][][][][][][] availableputplacestable; // （firstpuyoを置く場所、secondpuyoを置く場所）の組が入っている
	// PuyoDirection が Down のとき(availableactionstable[][][][][][][][][0] == 2 のとき)はsecondpuyoを先に置かないといけない。
	public int[] toptoindex;
	
	public  FastBitNextField() {
		// 各種テーブルなどの初期化を行う
		// まず得点を近似するテーブルの初期化も行っておく
		int[] tempbairitsutable = {0, 0, 8, 16, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 480, 512};
		scoretable = new int[20][72][5];
		for (int erasenum=4;erasenum<72;erasenum++) {
			double tempb1 = erasenum - 3; // 消したのが1色だけの場合
			if (erasenum == 4) {
				tempb1 = 0;
			}
			else if (erasenum > 10) {
				tempb1 = 10;
			}
			int tempb2 = erasenum - 3; // 2色消した場合
			if (erasenum < 10) {
				tempb2 = erasenum * 2 - 13;
			}
			int tempb3 = erasenum - 6; // 3色消した場合
			int tempb4 = erasenum - 4; // 4色消した場合
			int tempb5 = erasenum + 4; // 5色消した場合
			for (int i=1;i<20;i++) {
				int score1 = (int)(tempbairitsutable[i] + tempb1) * erasenum * 10;
				int score2 = (tempbairitsutable[i] + tempb2) * erasenum * 10;
				int score3 = (tempbairitsutable[i] + tempb3) * erasenum * 10;
				int score4 = (tempbairitsutable[i] + tempb4) * erasenum * 10;
				int score5 = (tempbairitsutable[i] + tempb5) * erasenum * 10;
				int[] tempsc = {score1, score2, score3, score4, score5};
				scoretable[i][erasenum] = tempsc;
				if (scoretable[i][erasenum][0] == 0) {
					scoretable[i][erasenum][0] = 40;
				}
			}
		}
		
		// 表面にあるぷよを消したときの得点と、消すまでに必要なぷよの数から、評価値を出す関数を設定する
		double[] tempmulti = {0, 0.5, 0.4, 0.25};
		firepossibilityevaluator = (firepos, numtof) -> firepos * tempmulti[numtof];
		
		// topindexを返す関数で参照するテーブルを作る
		topindextable = new int[ 64 ];
		long hash = 0x03F566ED27179461L;
		for ( int i = 0; i < 64; i++ )
		{
		    topindextable[(int)( hash >>> 58) & 0x3F] = (i + 5)/ 5;
		    hash <<= 1;
		}
		// これがないとtopindextable[0] が1になっているが、(0 * hash) >> 58 も(1 * hash) >> 58 も0になるので、
		// TopIndex() では特別に入力が0のときに0を返すようにする。
		
		// 次の行動を返す時に参照するテーブルを作る
		availableactionstable = new int[2][3][3][3][3][3][3][][];
		availableputplacestable = new int[2][3][3][3][3][3][3][][];
		toptoindex = new int[13];
		int[][] samecoloravacts = new int[11][2];
		int[][] samecolorptplcs = new int[11][2];
		int[][] difcoloravacts = new int[22][2];
		int[][] difcolorptplcs = new int[22][2];
		toptoindex[12] = 2;
		toptoindex[11] = 1;
		samecoloravacts[0][0] = 0;
		samecoloravacts[0][1] = 0;
		samecolorptplcs[0][0] = 0;
		samecolorptplcs[0][1] = 0;
		difcoloravacts[0][0] = 0;
		difcoloravacts[0][1] = 0;
		difcolorptplcs[0][0] = 0;
		difcolorptplcs[0][1] = 0;
		difcoloravacts[1][0] = 2;
		difcoloravacts[1][1] = 0;
		difcolorptplcs[1][0] = 0;
		difcolorptplcs[1][1] = 0;
		for (int i=0;i<5;i++) {
			samecoloravacts[i+1][0] = 0;
			samecoloravacts[i+1][1] = i+1;
			samecoloravacts[i+6][0] = 1;
			samecoloravacts[i+6][1] = i;
			samecolorptplcs[i+1][0] = i+1;
			samecolorptplcs[i+1][1] = i+1;
			samecolorptplcs[i+6][0] = i;
			samecolorptplcs[i+6][1] = i+1;

			difcoloravacts[i * 2 + 2][0] = 0;
			difcoloravacts[i * 2 + 2][1] = i+1;
			difcoloravacts[i * 2 + 3][0] = 2;
			difcoloravacts[i * 2 + 3][1] = i+1;
			difcoloravacts[i * 2 + 12][0] = 1;
			difcoloravacts[i * 2 + 12][1] = i;
			difcoloravacts[i * 2 + 13][0] = 3;
			difcoloravacts[i * 2 + 13][1] = i+1;
			difcolorptplcs[i * 2 + 2][0] = i+1;
			difcolorptplcs[i * 2 + 2][1] = i+1;
			difcolorptplcs[i * 2 + 3][0] = i+1;
			difcolorptplcs[i * 2 + 3][1] = i+1;
			difcolorptplcs[i * 2 + 12][0] = i;
			difcolorptplcs[i * 2 + 12][1] = i+1;
			difcolorptplcs[i * 2 + 13][0] = i+1;
			difcolorptplcs[i * 2 + 13][1] = i;
		}
		for (int i=0;i<3;i++) {
			for (int j=0;j<3;j++) {
				for (int k=0;k<3;k++) {
					for (int l=0;l<3;l++) {
						for (int m=0;m<3;m++) {
							for (int n=0;n<3;n++) {
								boolean[] useplaces = new boolean[11];
								if (i == 0) {
									useplaces[0] = true;
								}
								if (j == 0) {
									useplaces[1] = true;
								}
								if (k == 0) {
									useplaces[2] = true;
								}
								if (l == 0) {
									useplaces[3] = true;
								}
								if (m == 0) {
									useplaces[4] = true;
								}
								if (n == 0) {
									useplaces[5] = true;
								}
								if (i < 2 && j < 2) {
									useplaces[6] = true;
								}
								if (j < 2 && k < 2) {
									useplaces[7] = true;
								}
								if (k < 2 && l < 2) {
									useplaces[8] = true;
								}
								if (l < 2 && m < 2) {
									useplaces[9] = true;
								}
								if (m < 2 && n < 2) {
									useplaces[10] = true;
								}
								int tempactionnum = 0;
								for (int o=0;o<11;o++) {
									if (useplaces[o]) {
										tempactionnum++;
									}
								}
								int[][] sactions = new int[tempactionnum][2];
								int[][] splaces = new int[tempactionnum][2];
								int[][] dactions = new int[tempactionnum * 2][2];
								int[][] dplaces = new int[tempactionnum * 2][2];
								int tempactionnum2 = 0;
								for (int o=0;o<11;o++) {
									if (useplaces[o]) {
										sactions[tempactionnum2] = samecoloravacts[o];
										splaces[tempactionnum2] = samecolorptplcs[o];
										dactions[tempactionnum2 * 2] = difcoloravacts[o * 2];
										dactions[tempactionnum2 * 2 + 1] = difcoloravacts[o * 2 + 1];
										dplaces[tempactionnum2 * 2] = difcolorptplcs[o * 2];
										dplaces[tempactionnum2 * 2 + 1] = difcolorptplcs[o * 2 + 1];
										tempactionnum2++;
									}
								}
								availableactionstable[0][i][j][k][l][m][n] = sactions;
								availableactionstable[1][i][j][k][l][m][n] = dactions;
								availableputplacestable[0][i][j][k][l][m][n] = splaces;
								availableputplacestable[1][i][j][k][l][m][n] = dplaces;
							}
						}
					}
				}
			}
		}
	}
	
	public FieldInfo ReadField(Board board) {
		Field field = board.getField();
		FieldInfo output = new FieldInfo();
		for (int i=0;i<12;i++) {
			for (int j=0;j<6;j++) {
				if (field.getPuyoType(j, i) == PuyoType.OJAMA_PUYO) {
					output.beforeojama[j] |= (long)1 << (i * 5);
				}
				else {
					output.beforefield[j] |= (long)ReadPuyoType(field.getPuyoType(j, i)) << (i * 5);
				}
			}
		}
		output.firstpuyo = IndexofPuyoType(board.getCurrentPuyo().getPuyoType(PuyoNumber.FIRST));
		output.secondpuyo = IndexofPuyoType(board.getCurrentPuyo().getPuyoType(PuyoNumber.SECOND));
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
				return 4;
			case RED_PUYO:
				return 8;
			case YELLOW_PUYO:
				return 16;
			default:
				return 0;
			}
		}
	}
	
	public int IndexofPuyoType(PuyoType puyotype) {
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
	
	public boolean CalcNext(FieldInfo field) {
		// まず周囲4つとつながっているかを見る
		long a = field.beforefield[0];
		long b = field.beforefield[1];
		long c = field.beforefield[2];
		long d = field.beforefield[3];
		long e = field.beforefield[4];
		long f = field.beforefield[5];
		long ojamaa = field.beforeojama[0];
		long ojamab = field.beforeojama[1];
		long ojamac = field.beforeojama[2];
		long ojamad = field.beforeojama[3];
		long ojamae = field.beforeojama[4];
		long ojamaf = field.beforeojama[5];
		
		long am = a & b;
		long bm = b & c;
		long cm = c & d;
		long dm = d & e;
		long em = e & f;
		long au = a & (a >>> 5);
		long bu = b & (b >>> 5);
		long cu = c & (c >>> 5);
		long du = d & (d >>> 5);
		long eu = e & (e >>> 5);
		long fu = f & (f >>> 5);
		long mask = 37191016277640225L; // 最初と、そこから5ビットごとに1が立っている　61ビット目は例外的に0
		
		long tempam = am | (am >>> 1) | (am >>> 2);
		long amigi = (tempam | (tempam >>> 2)) & mask;
		long tempau = au | (au >>> 1) | (au >>> 2);
		long aue = (tempau | (tempau >>> 2)) & mask;
		long tempbm = bm | (bm >>> 1) | (bm >>> 2);
		long bmigi = (tempbm | (tempbm >>> 2)) & mask;
		long tempbu = bu | (bu >>> 1) | (bu >>> 2);
		long bue = (tempbu | (tempbu >>> 2)) & mask;
		long tempcm = cm | (cm >>> 1) | (cm >>> 2);
		long cmigi = (tempcm | (tempcm >>> 2)) & mask;
		long tempcu = cu | (cu >>> 1) | (cu >>> 2);
		long cue = (tempcu | (tempcu >>> 2)) & mask;
		long tempdm = dm | (dm >>> 1) | (dm >>> 2);
		long dmigi = (tempdm | (tempdm >>> 2)) & mask;
		long tempdu = du | (du >>> 1) | (du >>> 2);
		long due = (tempdu | (tempdu >>> 2)) & mask;
		long tempem = em | (em >>> 1) | (em >>> 2);
		long emigi = (tempem | (tempem >>> 2)) & mask;
		long tempeu = eu | (eu >>> 1) | (eu >>> 2);
		long eue = (tempeu | (tempeu >>> 2)) & mask;
		long tempfu = fu | (fu >>> 1) | (fu >>> 2);
		long fue = (tempfu | (tempfu >>> 2)) & mask;
		
		// 次に、4つつながっている場所を探す。
		// 5種類すべてのテトラミノは、ドミノの周囲6か所の内2か所に
		// 正方形がつながっている形で表せるので、この条件を満たすドミノをまず見つける。
		
		long tempamigi = amigi + (amigi >>> 5);
		long tempbmigi = bmigi + (bmigi >>> 5);
		long tempcmigi = cmigi + (cmigi >>> 5);
		long tempdmigi = dmigi + (dmigi >>> 5);
		long tempemigi = emigi + (emigi >>> 5);

		long atatedomino = (aue << 5) + (aue >>> 5) + tempamigi;
		long btatedomino = (bue << 5) + (bue >>> 5) + tempamigi + tempbmigi;
		long ctatedomino = (cue << 5) + (cue >>> 5) + tempbmigi + tempcmigi;
		long dtatedomino = (due << 5) + (due >>> 5) + tempcmigi + tempdmigi;
		long etatedomino = (eue << 5) + (eue >>> 5) + tempdmigi + tempemigi;
		long ftatedomino = (fue << 5) + (fue >>> 5) + tempemigi;
		
		atatedomino = ((atatedomino >>> 1) | (atatedomino >>> 2)) & aue;
		btatedomino = ((btatedomino >>> 1) | (btatedomino >>> 2)) & bue;
		ctatedomino = ((ctatedomino >>> 1) | (ctatedomino >>> 2)) & cue;
		dtatedomino = ((dtatedomino >>> 1) | (dtatedomino >>> 2)) & due;
		etatedomino = ((etatedomino >>> 1) | (etatedomino >>> 2)) & eue;
		ftatedomino = ((ftatedomino >>> 1) | (ftatedomino >>> 2)) & fue;
		
		long tempaue = aue + (aue << 5);
		long tempbue = bue + (bue << 5);
		long tempcue = cue + (cue << 5);
		long tempdue = due + (due << 5);
		long tempeue = eue + (eue << 5);
		long tempfue = fue + (fue << 5);
		
		long ayokodomino = bmigi + tempaue + tempbue;
		long byokodomino = amigi + cmigi + tempbue + tempcue;
		long cyokodomino = bmigi + dmigi + tempcue + tempdue;
		long dyokodomino = cmigi + emigi + tempdue + tempeue;
		long eyokodomino = dmigi + tempeue + tempfue;
		
		ayokodomino = ((ayokodomino >>> 1) | (ayokodomino >>> 2)) & amigi;
		byokodomino = ((byokodomino >>> 1) | (byokodomino >>> 2)) & bmigi;
		cyokodomino = ((cyokodomino >>> 1) | (cyokodomino >>> 2)) & cmigi;
		dyokodomino = ((dyokodomino >>> 1) | (dyokodomino >>> 2)) & dmigi;
		eyokodomino = ((eyokodomino >>> 1) | (eyokodomino >>> 2)) & emigi;
		
		// テトラミノ中のドミノを見つけたところで、そこを消える場所として記録する
		
		long erasea = atatedomino | (atatedomino << 5) | ayokodomino;
		long eraseb = btatedomino | (btatedomino << 5) | ayokodomino | byokodomino;	
		long erasec = ctatedomino | (ctatedomino << 5) | byokodomino | cyokodomino;
		long erased = dtatedomino | (dtatedomino << 5) | cyokodomino | dyokodomino;
		long erasee = etatedomino | (etatedomino << 5) | dyokodomino | eyokodomino;
		long erasef = ftatedomino | (ftatedomino << 5) | eyokodomino;

		// 消える場所とつながっている場所を消える場所として記録する
		
		erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5) | (eraseb & amigi);
		eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5) | (erasea & amigi) | (erasec & bmigi);
		erasec |= ((erasec >>> 5) & cue) | ((erasec & cue) << 5) | (eraseb & bmigi) | (erased & cmigi);
		erased |= ((erased >>> 5) & due) | ((erased & due) << 5) | (erasec & cmigi) | (erasee & dmigi);
		erasee |= ((erasee >>> 5) & eue) | ((erasee & eue) << 5) | (erased & dmigi) | (erasef & emigi);
		erasef |= ((erasef >>> 5) & fue) | ((erasef & fue) << 5) | (erasee & emigi);
		
		// erasea~fをいじる前に得点を計算しておく
		int erasenum = Long.bitCount(erasea) + Long.bitCount(eraseb) + Long.bitCount(erasec) + Long.bitCount(erased) + Long.bitCount(erasee) + Long.bitCount(erasef);
		if (erasenum == 0) {
			return false;
		}
		else {
			// 消える色の数を見る
			long eraseamask = erasea | (erasea << 1);
			eraseamask |= (eraseamask << 2) | (erasea << 4);
			long erasebmask = eraseb | (eraseb << 1);
			erasebmask |= (erasebmask << 2) | (eraseb << 4);
			long erasecmask = erasec | (erasec << 1);
			erasecmask |= (erasecmask << 2) | (erasec << 4);
			long erasedmask = erased | (erased << 1);
			erasedmask |= (erasedmask << 2) | (erased << 4);
			long eraseemask = erasee | (erasee << 1);
			eraseemask |= (eraseemask << 2) | (erasee << 4);
			long erasefmask = erasef | (erasef << 1);
			erasefmask |= (erasefmask << 2) | (erasef << 4);
			long tempdata = (a & eraseamask) | (b & erasebmask) | (c & erasecmask) | (d & erasedmask) | (e & eraseemask) | (f & erasefmask);
			tempdata |= tempdata >>> 30;
		    tempdata |= tempdata >>> 15;
			tempdata |= (tempdata >>> 5) | (tempdata >>> 10);
			int erasecolornum = Long.bitCount(tempdata & (long)31);
			field.score += scoretable[field.nrensa][erasenum][erasecolornum - 1];
			field.nrensa++;
			// 消える場所の隣のおじゃまも消していく
			
			erasea |= ojamaa & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= ojamab & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= ojamac & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= ojamad & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= ojamae & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= ojamaf & ((erasef << 5) | (erasef >>> 5) | erasee);
			
			// ぷよを落とす
			// pextが使えればよかったのだが、やり方が分からない。
			// magic bitboard におけるmagic numberを使うと、ぷよの順番が保たれないのでこれは使えない。
			// 以上よりループを使う。
			
			while (erasea != 0) {
				long temperase = erasea & (-erasea);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = a & moveplace;
				long mvo = ojamaa & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				a = (mv >>> 5) | (a & saveplace);
				ojamaa = (mvo >>> 5) | (ojamaa & saveplace);
				erasea ^= temperase;
				erasea >>>= 5;
			}
			while (eraseb != 0) {
				long temperase = eraseb & (-eraseb);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = b & moveplace;
				long mvo = ojamab & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				b = (mv >>> 5) | (b & saveplace);
				ojamab = (mvo >>> 5) | (ojamab & saveplace);
				eraseb ^= temperase;
				eraseb >>>= 5;
			}
			while (erasec != 0) {
				long temperase = erasec & (-erasec);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = c & moveplace;
				long mvo = ojamac & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				c = (mv >>> 5) | (c & saveplace);
				ojamac = (mvo >>> 5) | (ojamac & saveplace);
				erasec ^= temperase;
				erasec >>>= 5;
			}
			while (erased != 0) {
				long temperase = erased & (-erased);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = d & moveplace;
				long mvo = ojamad & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				d = (mv >>> 5) | (d & saveplace);
				ojamad = (mvo >>> 5) | (ojamad & saveplace);
				erased ^= temperase;
				erased >>>= 5;
			}
			while (erasee != 0) {
				long temperase = erasee & (-erasee);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = e & moveplace;
				long mvo = ojamae & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				e = (mv >>> 5) | (e & saveplace);
				ojamae = (mvo >>> 5) | (ojamae & saveplace);
				erasee ^= temperase;
				erasee >>>= 5;
			}
			while (erasef != 0) {
				long temperase = erasef & (-erasef);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				long mv = f & moveplace;
				long mvo = ojamaf & moveplace;
				mv &= mv - 1;
				mvo &= mvo - 1;
				f = (mv >>> 5) | (f & saveplace);
				ojamaf = (mvo >>> 5) | (ojamaf & saveplace);
				erasef ^= temperase;
				erasef >>>= 5;
			}
			field.beforefield[0] = a;
			field.beforefield[1] = b;
			field.beforefield[2] = c;
			field.beforefield[3] = d;
			field.beforefield[4] = e;
			field.beforefield[5] = f;
			field.beforeojama[0] = ojamaa;
			field.beforeojama[1] = ojamab;
			field.beforeojama[2] = ojamac;
			field.beforeojama[3] = ojamad;
			field.beforeojama[4] = ojamae;
			field.beforeojama[5] = ojamaf;
			return true;
		}
	}
	
	public void ThinkNextActions(FieldInfo field) {
		// 可能手の探索
		long a = field.afterfield[0];
		long b = field.afterfield[1];
		long c = field.afterfield[2];
		long d = field.afterfield[3];
		long e = field.afterfield[4];
		long f = field.afterfield[5];
		long ojamaa = field.afterojama[0];
		long ojamab = field.afterojama[1];
		long ojamac = field.afterojama[2];
		long ojamad = field.afterojama[3];
		long ojamae = field.afterojama[4];
		long ojamaf = field.afterojama[5];
		int atop = TopIndex(a | ojamaa); // この列に何もなかったら0, 1段目にあったら1
		int btop = TopIndex(b | ojamab);
		int ctop = TopIndex(c | ojamac);
		int dtop = TopIndex(d | ojamad);
		int etop = TopIndex(e | ojamae);
		int ftop = TopIndex(f | ojamaf);
		int at = toptoindex[atop];
		int bt = toptoindex[btop];
		int ct = toptoindex[ctop];
		int dt = toptoindex[dtop];
		int et = toptoindex[etop];
		int ft = toptoindex[ftop];
		// 可能手の探索で使うのは一番上が10段目以下か、11段目か、12段目かの3通りと、
		// 次のぷよが2つとも同じ色かどうかのみなので、
		// 3^6 * 2 = 1458通りの場合における行動を記したテーブルを作って、それを参照する。
		int[][] putplaces;
		int nextfirstpuyo = field.firstpuyo;
		int nextsecondpuyo = field.secondpuyo;
		if (nextfirstpuyo == nextsecondpuyo) {
			field.availableactions = availableactionstable[0][at][bt][ct][dt][et][ft];
			putplaces = availableputplacestable[0][at][bt][ct][dt][et][ft];
		}
		else {
			field.availableactions = availableactionstable[1][at][bt][ct][dt][et][ft];
			putplaces = availableputplacestable[1][at][bt][ct][dt][et][ft];
		}
		long[] tempfield = {a, b, c, d, e, f};
		field.fieldafteravailableactions = new long[field.availableactions.length][6];
		// atopb5m1 = atop * 5 - 1
		//                  = atop * 4 + atop * 2 + (-atop - 1)
		//                  = (atop << 2) + (atop << 1) + (~atop)
		// あと足し算を分解すればもう少し早くなるかもしれない
		int atopb5m1 = (atop <<  2) + (atop << 1) + (~atop);
		int btopb5m1 = (btop <<  2) + (btop << 1) + (~btop);
		int ctopb5m1 = (ctop <<  2) + (ctop << 1) + (~ctop);
		int dtopb5m1 = (dtop <<  2) + (dtop << 1) + (~dtop);
		int etopb5m1 = (etop <<  2) + (etop << 1) + (~etop);
		int ftopb5m1 = (ftop <<  2) + (ftop << 1) + (~ftop);
		int[] temptops = {atopb5m1, btopb5m1, ctopb5m1, dtopb5m1, etopb5m1, ftopb5m1};
		for (int i=0;i<field.availableactions.length;i++) {
			for (int j=0;j<6;j++) {
				field.fieldafteravailableactions[i][j] = tempfield[j];
			}
			if (field.availableactions[i][0] == 2) {
				// secondpuyoを先に置くとき
				field.fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextsecondpuyo)) | ((long)1 << (temptops[putplaces[i][0]] + 5 + nextfirstpuyo));
			}
			else if (field.availableactions[i][0] == 0) {
				// firstpuyoを先に置くとき
				field.fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextfirstpuyo)) | ((long)1 << (temptops[putplaces[i][1]] + 5 + nextsecondpuyo));
			}
			else {
				// 横に置くとき
				field.fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextfirstpuyo));
				field.fieldafteravailableactions[i][putplaces[i][1]] |= ((long)1 << (temptops[putplaces[i][1]] + nextsecondpuyo));
			}
		}
	}
	
	public void ThinkFirePossibility(FieldInfo field) {
		// 表面にあるぷよをそれぞれ消してみて、どれくらいの点数になるかを見る
		// まず各列の最初のビットを取り出す
		long a = field.afterfield[0];
		long b = field.afterfield[1];
		long c = field.afterfield[2];
		long d = field.afterfield[3];
		long e = field.afterfield[4];
		long f = field.afterfield[5];
		long ojamaa = field.afterojama[0];
		long ojamab = field.afterojama[1];
		long ojamac = field.afterojama[2];
		long ojamad = field.afterojama[3];
		long ojamae = field.afterojama[4];
		long ojamaf = field.afterojama[5];
		long mask = 37191016277640225L;
		long atoprised = TopRised(a | ojamaa);
		long btoprised = TopRised(b | ojamab);
		long ctoprised = TopRised(c | ojamac);
		long dtoprised = TopRised(d | ojamad);
		long etoprised = TopRised(e | ojamae);
		long ftoprised = TopRised(f | ojamaf);
		// 表面の場所を取り出す
		long afilled = atoprised == 0 ? 0 : ((~atoprised) ^ (-atoprised)) & mask;
		long bfilled = btoprised == 0 ? 0 : ((~btoprised) ^ (-btoprised)) & mask;
		long cfilled = ctoprised == 0 ? 0 : ((~ctoprised) ^ (-ctoprised)) & mask;
		long dfilled = dtoprised == 0 ? 0 : ((~dtoprised) ^ (-dtoprised)) & mask;
		long efilled = etoprised == 0 ? 0 : ((~etoprised) ^ (-etoprised)) & mask;
		long ffilled = ftoprised == 0 ? 0 : ((~ftoprised) ^ (-ftoprised)) & mask;
		long asurface = (afilled & (afilled ^ bfilled)) | (a > ojamaa ? ((long)1 << (TopIndex(a) * 5)) : 0);
		long bsurface = (bfilled & ((afilled ^ bfilled) | (bfilled ^ cfilled))) | (b > ojamab ? ((long)1 << (TopIndex(b) * 5)) : 0);
		long csurface = (cfilled & ((bfilled ^ cfilled) | (cfilled ^ dfilled))) | (c > ojamac ? ((long)1 << (TopIndex(c) * 5)) : 0);
		long dsurface = (dfilled & ((cfilled ^ dfilled) | (dfilled ^ efilled))) | (d > ojamad ? ((long)1 << (TopIndex(d) * 5)) : 0);
		long esurface = (efilled & ((dfilled ^ efilled) | (efilled ^ ffilled))) | (e > ojamae ? ((long)1 << (TopIndex(e) * 5)) : 0);
		long fsurface = (ffilled & (efilled ^ ffilled)) | (f > ojamaf ? ((long)1 << (TopIndex(f) * 5)) : 0);
		
		// 後でつながっているぷよを消すために、どこがつながっているかを見る
		long am = a & b;
		long bm = b & c;
		long cm = c & d;
		long dm = d & e;
		long em = e & f;
		long au = a & (a >>> 5);
		long bu = b & (b >>> 5);
		long cu = c & (c >>> 5);
		long du = d & (d >>> 5);
		long eu = e & (e >>> 5);
		long fu = f & (f >>> 5);
		long tempam = am | (am >>> 1) | (am >>> 2);
		long amigi = (tempam | (tempam >>> 2)) & mask;
		long tempau = au | (au >>> 1) | (au >>> 2);
		long aue = (tempau | (tempau >>> 2)) & mask;
		long tempbm = bm | (bm >>> 1) | (bm >>> 2);
		long bmigi = (tempbm | (tempbm >>> 2)) & mask;
		long tempbu = bu | (bu >>> 1) | (bu >>> 2);
		long bue = (tempbu | (tempbu >>> 2)) & mask;
		long tempcm = cm | (cm >>> 1) | (cm >>> 2);
		long cmigi = (tempcm | (tempcm >>> 2)) & mask;
		long tempcu = cu | (cu >>> 1) | (cu >>> 2);
		long cue = (tempcu | (tempcu >>> 2)) & mask;
		long tempdm = dm | (dm >>> 1) | (dm >>> 2);
		long dmigi = (tempdm | (tempdm >>> 2)) & mask;
		long tempdu = du | (du >>> 1) | (du >>> 2);
		long due = (tempdu | (tempdu >>> 2)) & mask;
		long tempem = em | (em >>> 1) | (em >>> 2);
		long emigi = (tempem | (tempem >>> 2)) & mask;
		long tempeu = eu | (eu >>> 1) | (eu >>> 2);
		long eue = (tempeu | (tempeu >>> 2)) & mask;
		long tempfu = fu | (fu >>> 1) | (fu >>> 2);
		long fue = (tempfu | (tempfu >>> 2)) & mask;
		
		long tempconnect = amigi | (bmigi << 1) | (cmigi << 2) | (dmigi << 3) | (emigi << 4);
		long tempconnect2 = aue | (bue << 1) | (cue << 2) | (due << 3) | (eue << 4);
		int connectnum = Long.bitCount(tempconnect) + Long.bitCount(tempconnect2) + Long.bitCount(fue);
		
		// 各表面のぷよについてそれぞれ消してみる
		// その前にいったん今の盤面を保存しておく
		long[] savedfield = {a, b, c, d, e, f};
		long[] savedojama = {ojamaa, ojamab, ojamac, ojamad, ojamae, ojamaf};
		double maxfirepossibility = 0;
		field.numtofire = 0;
		
		while (asurface != 0) {
			// 表面のぷよの位置を一つ取り出す
			long erasea = asurface & (-asurface);
			
			// つながっているのが最大３ぷよなので、周囲25マスを見る
			erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 一つ上と一つ下を見る
			erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 二つ上から二つ下までを見る
			long eraseb = erasea & amigi;
			long erasec = eraseb & bmigi;
			// 発火に必要なぷよの数を調べる
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec);  

			// おじゃまを消す
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb);
			
			// 既に消したところは、今後消してみる位置のリストから外す
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			
			// ぷよを落とす
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = {savedfield[3], savedojama[3]};
			long[] es = {savedfield[4], savedojama[4]};
			long[] fs = {savedfield[5], savedojama[5]};
			
			// 連鎖を計算する
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		
		while (bsurface != 0) {
			long eraseb = bsurface & (-bsurface);
			eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5);
			eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5);
			long erasea = eraseb & amigi;
			long erasec = eraseb & bmigi;
			long erased = erasec & cmigi;
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased);  
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec);
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = {savedfield[4], savedojama[4]};
			long[] fs = {savedfield[5], savedojama[5]};
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		
		while (csurface != 0) {
			long erasec = csurface & (-csurface);
			erasec |= ((erasec >>> 5) & cue) | ((erasec & cue) << 5);
			erasec |= ((erasec >>> 5) & cue) | ((erasec & cue) << 5);
			long eraseb = erasec & bmigi;
			long erasea = eraseb & amigi;
			long erased = erasec & cmigi;
			long erasee = erased & dmigi;
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee);  
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased);
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = {savedfield[5], savedojama[5]};
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		
		while (dsurface != 0) {
			long erased = dsurface & (-dsurface);
			erased |= ((erased >>> 5) & due) | ((erased & due) << 5);
			erased |= ((erased >>> 5) & due) | ((erased & due) << 5);
			long erasec = erased & cmigi;
			long eraseb = erasec & bmigi;
			long erasee = erased & dmigi;
			long erasef = erasee & emigi;
			int tempnumtofire = 4 - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		
		while (esurface != 0) {
			long erasee = esurface & (-esurface);
			erasee |= ((erasee >>> 5) & eue) | ((erasee & eue) << 5);
			erasee |= ((erasee >>> 5) & eue) | ((erasee & eue) << 5);
			long erased = erasee & dmigi;
			long erasec = erased & cmigi;
			long erasef = erasee & emigi;
			int tempnumtofire = 4 - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = {savedfield[1], savedojama[1]};
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		
		while (fsurface != 0) {
			long erasef = fsurface & (-fsurface);
			erasef |= ((erasef >>> 5) & fue) | ((erasef & fue) << 5);
			erasef |= ((erasef >>> 5) & fue) | ((erasef & fue) << 5);
			long erasee = erasef & emigi;
			long erased = erasee & dmigi;		
			int tempnumtofire = 4 - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = {savedfield[1], savedojama[1]};
			long[] cs = {savedfield[2], savedojama[2]};
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
		}
		field.firepossibility = maxfirepossibility + connectnum;
	}
	
	public void Calc(FieldInfo field, boolean thinknextactions, boolean thinkfirepossibility) {
		while (CalcNext(field)) {
		}
		long gotta = 0;
		for (int i=0;i<6;i++) {
			gotta |= field.beforefield[i] | field.beforeojama[i];
		}
		if (gotta == 0) {
			field.score += 2100;
		}
		field.afterfield = Arrays.copyOf(field.beforefield, field.beforefield.length);
		field.afterojama = Arrays.copyOf(field.beforeojama, field.beforeojama.length);
		if (thinknextactions) {
			ThinkNextActions(field);
		}
		if (thinkfirepossibility) {
			ThinkFirePossibility(field);
		}
	}
	
	public int TopIndex(long data) {
		if (data == 0) {
			return 0;
		}
		else {
			data = (data & 0xFFFFFFFF00000000L) != 0 ? (data & 0xFFFFFFFF00000000L) : data;
			data = (data & 0xFFFF0000FFFF0000L) != 0 ? (data & 0xFFFF0000FFFF0000L) : data;
			data = (data & 0xFF00FF00FF00FF00L) != 0 ? (data & 0xFF00FF00FF00FF00L) : data;
			data = (data & 0xF0F0F0F0F0F0F0F0L) != 0 ? (data & 0xF0F0F0F0F0F0F0F0L) : data;
			data = (data & 0xCCCCCCCCCCCCCCCCL) != 0 ? (data & 0xCCCCCCCCCCCCCCCCL) : data;
			data = (data & 0xAAAAAAAAAAAAAAAAL) != 0 ? (data & 0xAAAAAAAAAAAAAAAAL) : data;
			// magic number を用いて右からn個目の位置のビットのみが立っているlongに対してnを求める
			long index = (((data * 0x03F566ED27179461L) >>> 58) & 0x3F);
			return topindextable[(int) index];
		}
	}
	
	public long TopRised(long data) {
		if (data == 0) {
			return 0;
		}
		else {
			data = (data & 0xFFFFFFFF00000000L) != 0 ? (data & 0xFFFFFFFF00000000L) : data;
			data = (data & 0xFFFF0000FFFF0000L) != 0 ? (data & 0xFFFF0000FFFF0000L) : data;
			data = (data & 0xFF00FF00FF00FF00L) != 0 ? (data & 0xFF00FF00FF00FF00L) : data;
			data = (data & 0xF0F0F0F0F0F0F0F0L) != 0 ? (data & 0xF0F0F0F0F0F0F0F0L) : data;
			data = (data & 0xCCCCCCCCCCCCCCCCL) != 0 ? (data & 0xCCCCCCCCCCCCCCCCL) : data;
			data = (data & 0xAAAAAAAAAAAAAAAAL) != 0 ? (data & 0xAAAAAAAAAAAAAAAAL) : data;
			return data;
		}
	}
	

	public long[] FallDownPuyo(long inputx, long inputy, long eraseplace) {
		if (inputy == 0) {
			// おじゃまがない場合
			while (eraseplace != 0) {
				long temperase = eraseplace & (-eraseplace);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				inputx = inputx >>> 5 & moveplace | (inputx & saveplace);
				eraseplace ^= temperase;
				eraseplace >>>= 5;
			}
		}
		else {
			while (eraseplace != 0) {
				long temperase = eraseplace & (-eraseplace);
				long moveplace = -temperase;
				long saveplace = ~moveplace;
				inputx = inputx >>> 5 & moveplace | (inputx & saveplace);
				inputy = inputy >>> 5 & moveplace | (inputy & saveplace);
				eraseplace ^= temperase;
				eraseplace >>>= 5;
			}
		}
		long[] output = {inputx, inputy};
		return output;
	}
	
	public FieldInfo Tsumos(FieldInfo field, int tsumoindex){
		int firstpuyo = 0;
		int secondpuyo = 0;
		if (tsumoindex < 10) {
			if (tsumoindex < 4) {
				firstpuyo = 1;
				secondpuyo = tsumoindex + 2;
			}
			else if (tsumoindex < 7) {
				firstpuyo = 2;
				secondpuyo = tsumoindex - 1;
			}
			else if (tsumoindex < 9) {
				firstpuyo = 3;
				secondpuyo = tsumoindex - 3;
			}
			else {
				firstpuyo = 4;
				secondpuyo = 5;
			}
		}
		FieldInfo output = new FieldInfo(field, firstpuyo, secondpuyo);
		ThinkNextActions(output);
		return output;
	}
	
	public void ThinkFirePossibilitywithPlacetoFire(FieldInfo field) {
		// 表面にあるぷよをそれぞれ消してみて、どれくらいの点数になるかを見る
		// まず各列の最初のビットを取り出す
		long a = field.afterfield[0];
		long b = field.afterfield[1];
		long c = field.afterfield[2];
		long d = field.afterfield[3];
		long e = field.afterfield[4];
		long f = field.afterfield[5];
		long ojamaa = field.afterojama[0];
		long ojamab = field.afterojama[1];
		long ojamac = field.afterojama[2];
		long ojamad = field.afterojama[3];
		long ojamae = field.afterojama[4];
		long ojamaf = field.afterojama[5];
		long mask = 37191016277640225L;
		long atoprised = TopRised(a | ojamaa);
		long btoprised = TopRised(b | ojamab);
		long ctoprised = TopRised(c | ojamac);
		long dtoprised = TopRised(d | ojamad);
		long etoprised = TopRised(e | ojamae);
		long ftoprised = TopRised(f | ojamaf);
		// 表面の場所を取り出す
		long afilled = atoprised == 0 ? 0 : ((~atoprised) ^ (-atoprised)) & mask;
		long bfilled = btoprised == 0 ? 0 : ((~btoprised) ^ (-btoprised)) & mask;
		long cfilled = ctoprised == 0 ? 0 : ((~ctoprised) ^ (-ctoprised)) & mask;
		long dfilled = dtoprised == 0 ? 0 : ((~dtoprised) ^ (-dtoprised)) & mask;
		long efilled = etoprised == 0 ? 0 : ((~etoprised) ^ (-etoprised)) & mask;
		long ffilled = ftoprised == 0 ? 0 : ((~ftoprised) ^ (-ftoprised)) & mask;
		long asurface = (afilled & (afilled ^ bfilled)) | (a > ojamaa ? ((long)1 << (TopIndex(a) * 5)) : 0);
		long bsurface = (bfilled & ((afilled ^ bfilled) | (bfilled ^ cfilled))) | (b > ojamab ? ((long)1 << (TopIndex(b) * 5)) : 0);
		long csurface = (cfilled & ((bfilled ^ cfilled) | (cfilled ^ dfilled))) | (c > ojamac ? ((long)1 << (TopIndex(c) * 5)) : 0);
		long dsurface = (dfilled & ((cfilled ^ dfilled) | (dfilled ^ efilled))) | (d > ojamad ? ((long)1 << (TopIndex(d) * 5)) : 0);
		long esurface = (efilled & ((dfilled ^ efilled) | (efilled ^ ffilled))) | (e > ojamae ? ((long)1 << (TopIndex(e) * 5)) : 0);
		long fsurface = (ffilled & (efilled ^ ffilled)) | (f > ojamaf ? ((long)1 << (TopIndex(f) * 5)) : 0);
		
		// 後でつながっているぷよを消すために、どこがつながっているかを見る
		long am = a & b;
		long bm = b & c;
		long cm = c & d;
		long dm = d & e;
		long em = e & f;
		long au = a & (a >>> 5);
		long bu = b & (b >>> 5);
		long cu = c & (c >>> 5);
		long du = d & (d >>> 5);
		long eu = e & (e >>> 5);
		long fu = f & (f >>> 5);
		long tempam = am | (am >>> 1) | (am >>> 2);
		long amigi = (tempam | (tempam >>> 2)) & mask;
		long tempau = au | (au >>> 1) | (au >>> 2);
		long aue = (tempau | (tempau >>> 2)) & mask;
		long tempbm = bm | (bm >>> 1) | (bm >>> 2);
		long bmigi = (tempbm | (tempbm >>> 2)) & mask;
		long tempbu = bu | (bu >>> 1) | (bu >>> 2);
		long bue = (tempbu | (tempbu >>> 2)) & mask;
		long tempcm = cm | (cm >>> 1) | (cm >>> 2);
		long cmigi = (tempcm | (tempcm >>> 2)) & mask;
		long tempcu = cu | (cu >>> 1) | (cu >>> 2);
		long cue = (tempcu | (tempcu >>> 2)) & mask;
		long tempdm = dm | (dm >>> 1) | (dm >>> 2);
		long dmigi = (tempdm | (tempdm >>> 2)) & mask;
		long tempdu = du | (du >>> 1) | (du >>> 2);
		long due = (tempdu | (tempdu >>> 2)) & mask;
		long tempem = em | (em >>> 1) | (em >>> 2);
		long emigi = (tempem | (tempem >>> 2)) & mask;
		long tempeu = eu | (eu >>> 1) | (eu >>> 2);
		long eue = (tempeu | (tempeu >>> 2)) & mask;
		long tempfu = fu | (fu >>> 1) | (fu >>> 2);
		long fue = (tempfu | (tempfu >>> 2)) & mask;
		
		long tempconnect = amigi | (bmigi << 1) | (cmigi << 2) | (dmigi << 3) | (emigi << 4);
		long tempconnect2 = aue | (bue << 1) | (cue << 2) | (due << 3) | (eue << 4);
		int connectnum = Long.bitCount(tempconnect) + Long.bitCount(tempconnect2) + Long.bitCount(fue);
		
		// 4手目で発火できるものを絞るために、表面の形を見る
		/*
		int topamigi = atoprised > btoprised ? 0 : 1;
		int topbhidari = btoprised > atoprised ? 0 : 1;
		int topbmigi = btoprised > ctoprised ? 0 : 1;
		int topchidari = ctoprised > btoprised ? 0 : 1;
		int topcmigi = ctoprised > dtoprised ? 0 : 1;
		int topdhidari = dtoprised > ctoprised ? 0 : 1;
		int topdmigi = dtoprised > etoprised ? 0 : 1;
		int topehidari = etoprised > dtoprised ? 0 : 1;
		int topemigi = etoprised > ftoprised ? 0 : 1;
		int topfhidari = ftoprised > etoprised ? 0 : 1;
		*/
		
		// 各表面のぷよについてそれぞれ消してみる
		// その前にいったん今の盤面を保存しておく
		long[] savedfield = {a, b, c, d, e, f};
		long[] savedojama = {ojamaa, ojamab, ojamac, ojamad, ojamae, ojamaf};
		double maxfirepossibility = 0;
		field.numtofire = 0;
		boolean[][] placetofire1= new boolean[5][6]; // 1個で発火できる場所
		boolean[][] placetofire2= new boolean[5][6]; // 2個で発火できる場所
		
		while (asurface != 0) {
			// 表面のぷよの位置を一つ取り出す
			long erasea = asurface & (-asurface);
			
			long savederase = erasea;

			// つながっているのが最大３ぷよなので、周囲25マスを見る
			erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 一つ上と一つ下を見る
			erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 二つ上から二つ下までを見る
			long eraseb = erasea & amigi;
			long erasec = eraseb & bmigi;
			// 発火に必要なぷよの数を調べる
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec);  

			boolean bfire = eraseb != 0;
			boolean cfire = erasec != 0;
			
			// おじゃまを消す
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb);
			
			// 既に消したところは、今後消してみる位置のリストから外す
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;

			// ぷよを落とす
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = {savedfield[3], savedojama[3]};
			long[] es = {savedfield[4], savedojama[4]};
			long[] fs = {savedfield[5], savedojama[5]};
			// 連鎖を計算する
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			// 4手目で発火できるツモと手を予め絞っておく
			if (tempfi.score > 0 && tempnumtofire < 3) {	
				// 消える色を見る
				long temp = (savederase * 31) & a;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][0] = true;
					if (bfire) {
						placetofire1[erasecolor-1][1] = true;
					}
					if (cfire) {
						placetofire1[erasecolor-1][2] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][0] = true;
					if (bfire) {
						placetofire2[erasecolor-1][1] = true;
					}
					if (cfire) {
						placetofire2[erasecolor-1][2] = true;
					}
				}
			}
		}
		
		while (bsurface != 0) {
			long eraseb = bsurface & (-bsurface);
			long savederase = eraseb;
			eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5);
			eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5);
			long erasea = eraseb & amigi;
			long erasec = eraseb & bmigi;
			long erased = erasec & cmigi;
			boolean afire = erasea != 0;
			boolean cfire = erasec != 0;
			boolean dfire = erased != 0;
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased);  
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec);
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = {savedfield[4], savedojama[4]};
			long[] fs = {savedfield[5], savedojama[5]};
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			if (tempfi.score > 0 && tempnumtofire < 3) {
				long temp = (savederase * 31) & b;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][1] = true;
					if (afire) {
						placetofire1[erasecolor-1][0] = true;
					}
					if (cfire) {
						placetofire1[erasecolor-1][2] = true;
					}
					if (dfire) {
						placetofire1[erasecolor-1][3] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][1] = true;
					if (afire) {
						placetofire2[erasecolor-1][0] = true;
					}
					if (cfire) {
						placetofire2[erasecolor-1][2] = true;
					}
					if (dfire) {
						placetofire2[erasecolor-1][3] = true;
					}
				}
			}
		}
		
		while (csurface != 0) {
			long erasec = csurface & (-csurface);
			long savederase = erasec;
			erasec |= ((erasec >>> 5) & cue) | ((erasec & cue) << 5);
			erasec |= ((erasec >>> 5) & cue) | ((erasec & cue) << 5);
			long eraseb = erasec & bmigi;
			long erasea = eraseb & amigi;
			long erased = erasec & cmigi;
			long erasee = erased & dmigi;
			boolean afire = erasea != 0;
			boolean bfire = eraseb != 0;
			boolean dfire = erased != 0;
			boolean efire = erasee != 0;
			
			int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee);  
			erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased);
			asurface ^= asurface & erasea;
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			long[] as = FallDownPuyo(savedfield[0], savedojama[0], erasea);
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = {savedfield[5], savedojama[5]};
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			if (tempfi.score > 0 && tempnumtofire < 3) {
				long temp = (savederase * 31) & c;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][2] = true;
					if (afire) {
						placetofire1[erasecolor-1][0] = true;
					}
					if (bfire) {
						placetofire1[erasecolor-1][1] = true;
					}
					if (dfire) {
						placetofire1[erasecolor-1][3] = true;
					}
					if (efire) {
						placetofire1[erasecolor-1][4] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][2] = true;
					if (afire) {
						placetofire2[erasecolor-1][0] = true;
					}
					if (bfire) {
						placetofire2[erasecolor-1][1] = true;
					}
					if (dfire) {
						placetofire2[erasecolor-1][3] = true;
					}
					if (efire) {
						placetofire2[erasecolor-1][4] = true;
					}
				}
			}
		}
		
		while (dsurface != 0) {
			long erased = dsurface & (-dsurface);
			long savederase = erased;
			erased |= ((erased >>> 5) & due) | ((erased & due) << 5);
			erased |= ((erased >>> 5) & due) | ((erased & due) << 5);
			long erasec = erased & cmigi;
			long eraseb = erasec & bmigi;
			long erasee = erased & dmigi;
			long erasef = erasee & emigi;
			boolean bfire = eraseb != 0;
			boolean cfire = erasec != 0;
			boolean efire = erasee != 0;
			boolean ffire = erasef != 0;
			int tempnumtofire = 4 - Long.bitCount(eraseb) - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasec);
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			bsurface ^= bsurface & eraseb;
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = FallDownPuyo(savedfield[1], savedojama[1], eraseb);
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			
			if (tempfi.score > 0 && tempnumtofire < 3) {
				long temp = (savederase * 31) & d;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][3] = true;
					if (bfire) {
						placetofire1[erasecolor-1][1] = true;
					}
					if (cfire) {
						placetofire1[erasecolor-1][2] = true;
					}
					if (efire) {
						placetofire1[erasecolor-1][4] = true;
					}
					if (ffire) {
						placetofire1[erasecolor-1][5] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][3] = true;
					if (bfire) {
						placetofire2[erasecolor-1][1] = true;
					}
					if (cfire) {
						placetofire2[erasecolor-1][2] = true;
					}
					if (efire) {
						placetofire2[erasecolor-1][4] = true;
					}
					if (ffire) {
						placetofire2[erasecolor-1][5] = true;
					}
				}
			}
		}
		
		while (esurface != 0) {
			long erasee = esurface & (-esurface);
			long savederase = erasee;
			erasee |= ((erasee >>> 5) & eue) | ((erasee & eue) << 5);
			erasee |= ((erasee >>> 5) & eue) | ((erasee & eue) << 5);
			long erased = erasee & dmigi;
			long erasec = erased & cmigi;
			long erasef = erasee & emigi;
			boolean cfire = erasec != 0;
			boolean dfire = erased != 0;
			boolean ffire = erasef != 0;
			int tempnumtofire = 4 - Long.bitCount(erasec) - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | erased);
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasec | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			csurface ^= csurface & erasec;
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = {savedfield[1], savedojama[1]};
			long[] cs = FallDownPuyo(savedfield[2], savedojama[2], erasec);
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			if (tempfi.score > 0 && tempnumtofire < 3) {
				long temp = (savederase * 31) & e;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][4] = true;
					if (cfire) {
						placetofire1[erasecolor-1][2] = true;
					}
					if (dfire) {
						placetofire1[erasecolor-1][3] = true;
					}
					if (ffire) {
						placetofire1[erasecolor-1][5] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][4] = true;
					if (cfire) {
						placetofire2[erasecolor-1][2] = true;
					}
					if (dfire) {
						placetofire2[erasecolor-1][3] = true;
					}
					if (ffire) {
						placetofire2[erasecolor-1][5] = true;
					}
				}
			}
		}
		
		while (fsurface != 0) {
			long erasef = fsurface & (-fsurface);
			long savederase = erasef;
			erasef |= ((erasef >>> 5) & fue) | ((erasef & fue) << 5);
			erasef |= ((erasef >>> 5) & fue) | ((erasef & fue) << 5);
			long erasee = erasef & emigi;
			long erased = erasee & dmigi;		
			boolean dfire = erased != 0;
			boolean efire = erasee != 0;
			int tempnumtofire = 4 - Long.bitCount(erased) - Long.bitCount(erasee) - Long.bitCount(erasef); 
			erased |= savedojama[3] & ((erased << 5) | (erased >>> 5) | erasee);
			erasee |= savedojama[4] & ((erasee << 5) | (erasee >>> 5) | erased | erasef);
			erasef |= savedojama[5] & ((erasef << 5) | (erasef >>> 5) | erasee);
			dsurface ^= dsurface & erased;
			esurface ^= esurface & erasee;
			fsurface ^= fsurface & erasef;
			long[] as = {savedfield[0], savedojama[0]};
			long[] bs = {savedfield[1], savedojama[1]};
			long[] cs = {savedfield[2], savedojama[2]};
			long[] ds = FallDownPuyo(savedfield[3], savedojama[3], erased);
			long[] es = FallDownPuyo(savedfield[4], savedojama[4], erasee);
			long[] fs = FallDownPuyo(savedfield[5], savedojama[5], erasef);
			FieldInfo tempfi = new FieldInfo(as, bs, cs, ds, es, fs);
			Calc(tempfi, false, false);
			double tempeva = firepossibilityevaluator.apply(tempfi.score + 40, tempnumtofire);
			if (tempeva > maxfirepossibility) {
				maxfirepossibility = tempeva;
			}
			if (tempfi.score > 0 && tempnumtofire < 3) {
				long temp = (savederase * 31) & f;
				temp = temp & (-temp);
				temp >>>= (TopIndex(temp) - 1) * 5;
				int erasecolor = temp == 1 ? 1 : temp == 2 ? 2 : temp == 4 ? 3 : temp == 8 ? 4 : temp == 16 ? 5 : 0;
				if (tempnumtofire == 1) {
					placetofire1[erasecolor-1][5] = true;
					if (dfire) {
						placetofire1[erasecolor-1][3] = true;
					}
					if (efire) {
						placetofire1[erasecolor-1][4] = true;
					}
				}
				else {
					placetofire2[erasecolor-1][5] = true;
					if (dfire) {
						placetofire2[erasecolor-1][3] = true;
					}
					if (efire) {
						placetofire2[erasecolor-1][4] = true;
					}
				}
			}
		}
		field.firepossibility = maxfirepossibility + connectnum;
		field.placetofire1 = placetofire1;
		field.placetofire2 = placetofire2;
	}
	
	public void CalcPlacetoFire(FieldInfo field) {
		while (CalcNext(field)) {
		}
		long gotta = 0;
		for (int i=0;i<6;i++) {
			gotta |= field.beforefield[i] | field.beforeojama[i];
		}
		if (gotta == 0) {
			field.score += 2100;
		}
		field.afterfield = Arrays.copyOf(field.beforefield, field.beforefield.length);
		field.afterojama = Arrays.copyOf(field.beforeojama, field.beforeojama.length);
		ThinkFirePossibilitywithPlacetoFire(field);
	}
	
	public void CalcPlacetoFire(FieldInfo field, int ojamanum, int befscore) {
		while (CalcNext(field)) {
		}
		long gotta = 0;
		for (int i=0;i<6;i++) {
			gotta |= field.beforefield[i] | field.beforeojama[i];
		}
		if (gotta == 0) {
			field.score += 2100;
		}
		int ojamadan = (ojamanum - (befscore + field.score) / 70 + 5) / 6;
		FallDownOjama(field, ojamadan);
		ThinkFirePossibilitywithPlacetoFire(field);
	}
	
	
	public int TsumotoTsumoIndex(int firstpuyo, int secondpuyo) {
		// firstpuyo, secondpuyo は0からはじまる
		switch (firstpuyo) {
		case 0:
			return secondpuyo;
		case 1:
			return secondpuyo + 4;
		case 2:
			return secondpuyo + 7;
		case 3:
			return secondpuyo + 9;
		case 4:
			return secondpuyo + 10;
		default:
			return 0;
		}
	}
	
	public int TsumotoTsumoIndex2(int firstpuyo, int secondpuyo) {
		return (-firstpuyo  + 7) * firstpuyo / 2 - 1 + secondpuyo;
	}
	
	public int[] ActiontoActionIndex(int firstpuyo, int secondpuyo, int firstcolumn, int secondcolumn, boolean isup) {
		int tsumoindex = TsumotoTsumoIndex(firstpuyo, secondpuyo);
		int[]output = {tsumoindex, 0};
		if (firstpuyo == secondpuyo) {
			output[1] = 220 + firstpuyo * 11;
			if (firstcolumn == secondcolumn) {
				output[1] += firstcolumn;
			}
			else {
				output[1] += Math.min(firstcolumn, secondcolumn) + 6;
			}
		}
		else {
			output[1] = TsumotoTsumoIndex2(firstpuyo, secondpuyo) * 22;
			if (firstcolumn == secondcolumn) {
				if (isup) {
					output[1] += firstcolumn;
				}
				else {
					output[1] += firstcolumn + 6;
				}
			}
			else {
				if (firstcolumn < secondcolumn) {
					output[1] += firstcolumn + 12;
				}
				else {
					output[1] += secondcolumn + 17;
				}
			}
		}
		return output;
	}
	
	public int[] Tsumos(FieldInfo field){
		boolean[] alreadythought = new boolean[275];
		int[] maxscores = new int[15];
		int[] tops = new int[6];
		for (int i=0;i<6;i++) {
			tops[i] = TopIndex(field.afterfield[i]) * 5;
		}
		
		for (int i=0;i<5;i++) {
			for (int j=0;j<6;j++) {
				if (field.placetofire2[i][j]) {
					if (j > 0) {
						int[] actionindex = ActiontoActionIndex(i, i, j, j-1, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, i, j, j-1, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
							 alreadythought[actionindex[1]] = true;
						}
					}
					if (j < 5) {
						int[] actionindex = ActiontoActionIndex(i, i, j, j+1, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, i, j, j+1, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
							 alreadythought[actionindex[1]] = true;
						}
					}
					int[] actionindex = ActiontoActionIndex(i, i, j, j, false);
					if (!alreadythought[actionindex[1]]) {
						 FieldInfo tempf = new FieldInfo(field, i, i, j, j, tops, false);
						 Calc(tempf, false, false);
						 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
						 alreadythought[actionindex[1]] = true;
					}
					actionindex = ActiontoActionIndex(i, i, j, j, true);
					if (!alreadythought[actionindex[1]]) {
						 FieldInfo tempf = new FieldInfo(field, i, i, j, j, tops, true);
						 Calc(tempf, false, false);
						 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
						 alreadythought[actionindex[1]] = true;
					}
				}
				if (field.placetofire1[i][j]) {
					for (int k=0;k<5;k++) {
						if (j > 0) {
							int[] actionindex = ActiontoActionIndex(i, k, j, j-1, false);
							if (!alreadythought[actionindex[1]]) {
								 FieldInfo tempf = new FieldInfo(field, i, k, j, j-1, tops, false);
								 Calc(tempf, false, false);
								 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
								 alreadythought[actionindex[1]] = true;
							}
						}
						if (j < 5) {
							int[] actionindex = ActiontoActionIndex(i, k, j, j+1, false);
							if (!alreadythought[actionindex[1]]) {
								 FieldInfo tempf = new FieldInfo(field, i, k, j, j+1, tops, false);
								 Calc(tempf, false, false);
								 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
								 alreadythought[actionindex[1]] = true;
							}
						}
						int[] actionindex = ActiontoActionIndex(i, k, j, j, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, k, j, j, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
							 alreadythought[actionindex[1]] = true;
						}
						actionindex = ActiontoActionIndex(i, k, j, j, true);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, k, j, j, tops, true);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score);
							 alreadythought[actionindex[1]] = true;
						}
					}
				}
			}
		}
		return maxscores;
	}
	
	public int[] TsumoswithOjamaDiscount(FieldInfo field){
		boolean[] alreadythought = new boolean[275];
		int[] maxscores = new int[15];
		int[] tops = new int[6];
		for (int i=0;i<6;i++) {
			tops[i] = TopIndex(field.afterfield[i]) * 5;
		}
		
		for (int i=0;i<5;i++) {
			for (int j=0;j<6;j++) {
				if (field.placetofire2[i][j]) {
					if (j > 0) {
						int[] actionindex = ActiontoActionIndex(i, i, j, j-1, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, i, j, j-1, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
							 alreadythought[actionindex[1]] = true;
						}
					}
					if (j < 5) {
						int[] actionindex = ActiontoActionIndex(i, i, j, j+1, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, i, j, j+1, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
							 alreadythought[actionindex[1]] = true;
						}
					}
					int[] actionindex = ActiontoActionIndex(i, i, j, j, false);
					if (!alreadythought[actionindex[1]]) {
						 FieldInfo tempf = new FieldInfo(field, i, i, j, j, tops, false);
						 Calc(tempf, false, false);
						 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
						 alreadythought[actionindex[1]] = true;
					}
					actionindex = ActiontoActionIndex(i, i, j, j, true);
					if (!alreadythought[actionindex[1]]) {
						 FieldInfo tempf = new FieldInfo(field, i, i, j, j, tops, true);
						 Calc(tempf, false, false);
						 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
						 alreadythought[actionindex[1]] = true;
					}
				}
				if (field.placetofire1[i][j]) {
					for (int k=0;k<5;k++) {
						if (j > 0) {
							int[] actionindex = ActiontoActionIndex(i, k, j, j-1, false);
							if (!alreadythought[actionindex[1]]) {
								 FieldInfo tempf = new FieldInfo(field, i, k, j, j-1, tops, false);
								 Calc(tempf, false, false);
								 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
								 alreadythought[actionindex[1]] = true;
							}
						}
						if (j < 5) {
							int[] actionindex = ActiontoActionIndex(i, k, j, j+1, false);
							if (!alreadythought[actionindex[1]]) {
								 FieldInfo tempf = new FieldInfo(field, i, k, j, j+1, tops, false);
								 Calc(tempf, false, false);
								 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
								 alreadythought[actionindex[1]] = true;
							}
						}
						int[] actionindex = ActiontoActionIndex(i, k, j, j, false);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, k, j, j, tops, false);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
							 alreadythought[actionindex[1]] = true;
						}
						actionindex = ActiontoActionIndex(i, k, j, j, true);
						if (!alreadythought[actionindex[1]]) {
							 FieldInfo tempf = new FieldInfo(field, i, k, j, j, tops, true);
							 Calc(tempf, false, false);
							 maxscores[actionindex[0]] = Math.max(maxscores[actionindex[0]], tempf.score + 2000 - OjamaDiscount(tempf));
							 alreadythought[actionindex[1]] = true;
						}
					}
				}
			}
		}
		return maxscores;
	}
	
	public void CompareScores(int[] maxscoreforeachtsumo, int[] maxscoresofthismove, int scoreofthismove) {
		for (int i=0;i<15;i++) {
			maxscoreforeachtsumo[i] = Math.max(maxscoreforeachtsumo[i], maxscoresofthismove[i] + scoreofthismove);
		}
	}
	
	public void FallDownOjama(FieldInfo field, int ojamadan) {
		long mask = 0x0084210842108421L;
		long ojama = mask >>> (60 - ojamadan * 5);
		for (int i=0;i<6;i++) {
			long filled = TopIndex((field.beforefield[i] | field.beforeojama[i])) * 5;
			field.beforeojama[i] |= ojama << filled;
			field.beforeojama[i] &= mask;
		}
		field.afterfield = Arrays.copyOf(field.beforefield, 6);
		field.afterojama = Arrays.copyOf(field.beforeojama, 6);
	}
	
	public int OjamaDiscount(FieldInfo field) {
		int output = 0;
		for (int i=0;i<6;i++) {
			output += Long.bitCount(field.afterojama[i]);
		}
		return output * 30;
	}
}
