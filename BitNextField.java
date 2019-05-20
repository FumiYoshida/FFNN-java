package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class BitNextField {
	// ビット演算で次のフィールドを考えるクラス
	
	public long a;
	public long b;
	public long c;
	public long d;
	public long e;
	public long f;
	
	public long[][] fieldafteravailableactions;
	public long[] ojamafield;
	public int[][] availableactions; 
	// availableactions[i][0] には(i+1)番目の可能な行動におけるぷよをrotateする回数が、
	// availableactions[i][1] には(i+1)番目の可能な行動におけるぷよを置く位置(columnnum)が入る。
	
	public long ojamaa;
	public long ojamab;
	public long ojamac;
	public long ojamad;
	public long ojamae;
	public long ojamaf;
	
	public int nrensa;
	public int score;
	public int firepossibility;
	public int numtofire;
	public BiFunction<Integer, Integer, Double> firepossibilityevaluator;
	public double firepossibilityevaluation;
	public int[][][] scoretable;
	public int[] topindextable;
	public int[][][][][][][][][] availableactionstable; // 大きさ(3*3*3*3*3*3*(0~11)*2, 3*3*3*3*3*3*(0~22)*2) = 48114(以下) (puyoをrotateさせる回数、置く場所)の組
	public int[][][][][][][][][] availableputplacestable; // （firstpuyoを置く場所、secondpuyoを置く場所）の組が入っている
	// PuyoDirection が Down のとき(availableactionstable[][][][][][][][][0] == 2 のとき)はsecondpuyoを先に置かないといけない。
	public int[] toptoindex;
	
	public  BitNextField() {
		// 各種テーブルなどの初期化を行う
		// まず得点を近似するテーブルの初期化も行っておく
		int[] tempbairitsutable = {0, 0, 8, 16, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 480, 512};
		scoretable = new int[20][72][5];
		for (int erasenum=4;erasenum<72;erasenum++) {
			int tempb1 = erasenum - 3; // 消したのが1色だけの場合
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
				int score1 = (tempbairitsutable[i] + tempb1) * erasenum * 10;
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
		firepossibilityevaluator = (firepos, numtof) -> firepos * 0.3 * (0.5 + Math.exp(1 - numtof));
		
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
	
	public void ReadField(Field field) {
		a = 0;
		b = 0;
		c = 0;
		d = 0;
		e = 0;
		f = 0;
		ojamaa = 0;
		ojamab = 0;
		ojamac = 0;
		ojamad = 0;
		ojamae = 0;
		ojamaf = 0;
		for (int i=0;i<12;i++) {
			a |= (long)ReadPuyoType(field.getPuyoType(0, i)) << (i * 5);
			b |= (long)ReadPuyoType(field.getPuyoType(1, i)) << (i * 5);
			c |= (long)ReadPuyoType(field.getPuyoType(2, i)) << (i * 5);
			d |= (long)ReadPuyoType(field.getPuyoType(3, i)) << (i * 5);
			e |= (long)ReadPuyoType(field.getPuyoType(4, i)) << (i * 5);
			f |= (long)ReadPuyoType(field.getPuyoType(5, i)) << (i * 5);
			
			if (field.getPuyoType(0, i) == PuyoType.OJAMA_PUYO) {
				ojamaa |= (long)1 << (i * 5);
			}
			if (field.getPuyoType(1, i) == PuyoType.OJAMA_PUYO) {
				ojamab |= (long)1 << (i * 5);
			}
			if (field.getPuyoType(2, i) == PuyoType.OJAMA_PUYO) {
				ojamac |= (long)1 << (i * 5);
			}
			if (field.getPuyoType(3, i) == PuyoType.OJAMA_PUYO) {
				ojamad |= (long)1 << (i * 5);
			}
			if (field.getPuyoType(4, i) == PuyoType.OJAMA_PUYO) {
				ojamae |= (long)1 << (i * 5);
			}
			if (field.getPuyoType(5, i) == PuyoType.OJAMA_PUYO) {
				ojamaf |= (long)1 << (i * 5);
			}
		}
		
		
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
	
	public boolean CalcNext() {
		// まず周囲4つとつながっているかを見る
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
			if (erasecolornum < 1) {
				int a00 = 0;
				int b00 = a00;
				
			}
			score += scoretable[nrensa][erasenum][erasecolornum - 1];
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
			return true;
		}
	}
	
	public void Calc(boolean thinknextactions, boolean thinkfirepossibility, boolean ispuyosamecolor, int nextfirstpuyo, int nextsecondpuyo) {
		if (nrensa >= 0) {
			// 初期化をする
			nrensa = 1;
		}
		else {
			// nrensaがマイナスのとき、それはnrensaを2から始めるという指示だと受け取る(firepossibilityの計算に用いる)
			nrensa = 2;
		}
		score = 0;
		while (CalcNext()) {
			nrensa++;
		}
		if ((a | b | c | d | e | f | ojamaa | ojamab | ojamac | ojamad | ojamae | ojamaf) == 0) {
			// 全消しの判定
			score += 2100;
		}
		
		if (thinknextactions) {
			// 可能手の探索
			long[] tempoj = {ojamaa, ojamab, ojamac, ojamad, ojamae, ojamaf};
			ojamafield = tempoj;
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
			if (ispuyosamecolor) {
				availableactions = availableactionstable[0][at][bt][ct][dt][et][ft];
				putplaces = availableputplacestable[0][at][bt][ct][dt][et][ft];
			}
			else {
				availableactions = availableactionstable[1][at][bt][ct][dt][et][ft];
				putplaces = availableputplacestable[1][at][bt][ct][dt][et][ft];
			}
			long[] tempfield = {a, b, c, d, e, f};
			fieldafteravailableactions = new long[availableactions.length][6];
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
			for (int i=0;i<availableactions.length;i++) {
				for (int j=0;j<6;j++) {
					fieldafteravailableactions[i][j] = tempfield[j];
				}
				if (availableactions[i][0] == 2) {
					// secondpuyoを先に置くとき
					fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextsecondpuyo)) | ((long)1 << (temptops[putplaces[i][0]] + 5 + nextfirstpuyo));
				}
				else if (availableactions[i][0] == 0) {
					// firstpuyoを先に置くとき
					fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextfirstpuyo)) | ((long)1 << (temptops[putplaces[i][1]] + 5 + nextsecondpuyo));
				}
				else {
					// 横に置くとき
					fieldafteravailableactions[i][putplaces[i][0]] |= ((long)1 << (temptops[putplaces[i][0]] + nextfirstpuyo));
					fieldafteravailableactions[i][putplaces[i][1]] |= ((long)1 << (temptops[putplaces[i][1]] + nextsecondpuyo));
				}
				// (long) のキャストなしだとintとされて31以上の右シフトをするとマイナスになる
			}
		}
		if (thinkfirepossibility) {
			// 表面にあるぷよをそれぞれ消してみて、どれくらいの点数になるかを見る
			// まず各列の最初のビットを取り出す
			long mask = 37191016277640225L;
			long atoprised = TopRised(a);
			long btoprised = TopRised(b);
			long ctoprised = TopRised(c);
			long dtoprised = TopRised(d);
			long etoprised = TopRised(e);
			long ftoprised = TopRised(b);
			// 表面の場所を取り出す
			long afilled = atoprised == 0 ? 0 : ((~atoprised) ^ (-atoprised)) & mask;
			long bfilled = btoprised == 0 ? 0 : ((~btoprised) ^ (-btoprised)) & mask;
			long cfilled = ctoprised == 0 ? 0 : ((~ctoprised) ^ (-ctoprised)) & mask;
			long dfilled = dtoprised == 0 ? 0 : ((~dtoprised) ^ (-dtoprised)) & mask;
			long efilled = etoprised == 0 ? 0 : ((~etoprised) ^ (-etoprised)) & mask;
			long ffilled = ftoprised == 0 ? 0 : ((~ftoprised) ^ (-ftoprised)) & mask;
			long asurface = (afilled & (afilled ^ bfilled)) | ((long)1 << (TopIndex(a) * 5));
			long bsurface = (bfilled & ((afilled ^ bfilled) | (bfilled ^ cfilled))) | ((long)1 << (TopIndex(b) * 5));
			long csurface = (cfilled & ((bfilled ^ cfilled) | (cfilled ^ dfilled))) | ((long)1 << (TopIndex(c) * 5));
			long dsurface = (dfilled & ((cfilled ^ dfilled) | (dfilled ^ efilled))) | ((long)1 << (TopIndex(d) * 5));
			long esurface = (efilled & ((dfilled ^ efilled) | (efilled ^ ffilled))) | ((long)1 << (TopIndex(e) * 5));
			long fsurface = (ffilled & (efilled ^ ffilled)) | ((long)1 << (TopIndex(f) * 5));
			
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
			
			// 各表面のぷよについてそれぞれ消してみる
			// その前にいったん今の盤面を保存しておく
			long[] savedfield = {a, b, c, d, e, f};
			long[] savedojama = {ojamaa, ojamab, ojamac, ojamad, ojamae, ojamaf};
			int savedscore = score;
			int maxfirepossibility = 0;
			numtofire = 0;
			
			while (asurface != 0) {
				// 表面のぷよの位置を一つ取り出す
				long erasea = asurface & (-asurface);
				
				// つながっているのが最大３ぷよなので、周囲25マスを見る
				erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 一つ上と一つ下を見る
				erasea |= ((erasea >>> 5) & aue) | ((erasea & aue) << 5); // 二つ上から二つ下までを見る
				long eraseb = erasea & amigi;
				long erasec = eraseb & bmigi;
				erasea |= savedojama[0] & ((erasea << 5) | (erasea >>> 5) | eraseb);
				eraseb |= savedojama[1] & ((eraseb << 5) | (eraseb >>> 5) | erasea | erasec);
				erasec |= savedojama[2] & ((erasec << 5) | (erasec >>> 5) | eraseb);
				// 発火に必要なぷよの数を調べる
				int tempnumtofire = 4 - Long.bitCount(erasea) - Long.bitCount(eraseb) - Long.bitCount(erasec);  

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
				
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				
				// 連鎖を計算する
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);
				if (tempnumtofire <  2) {
					int aa = 0;
					int bb = aa;
				}
				if (score + 40 > maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
				}
			}
			
			while (bsurface != 0) {
				long eraseb = bsurface & (-bsurface);
				eraseb |= ((eraseb >>> 5) & bue) | ((eraseb & bue) << 5);
				eraseb |= ((eraseb >>> 5) & cue) | ((eraseb & cue) << 5);
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
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);
				if (score + 40> maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
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
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);
				if (score + 40> maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
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
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);

				if (score + 40> maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
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
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);

				if (score + 40> maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
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
				long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
				long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
				Calc(tempfield, tempojama, false, false, false, 0, 0, true);

				if (score + 40> maxfirepossibility) {
					maxfirepossibility = score + 40;
					numtofire = tempnumtofire;
				}
			}
			if (maxfirepossibility > 10000) {
				int a00 = 0;
				int b00 = a00;
			}
			firepossibility = maxfirepossibility;
			score = savedscore;
		}
	}
	
	public void Calc(long[] field, long[] ojamafield, boolean thinknextactions, boolean thinkfirepossibility, boolean ispuyosamecolor, int nextfirstpuyo, int nextsecondpuyo) {
		a = field[0];
		b = field[1];
		c = field[2];
		d = field[3];
		e = field[4];
		f = field[5];
		ojamaa = ojamafield[0];
		ojamab = ojamafield[1];
		ojamac = ojamafield[2];
		ojamad = ojamafield[3];
		ojamae = ojamafield[4];
		ojamaf = ojamafield[5];
		Calc(thinknextactions, thinkfirepossibility, ispuyosamecolor, nextfirstpuyo, nextsecondpuyo);
	}
	
	public void Calc(long[] field, long[] ojamafield, boolean thinknextactions, boolean thinkfirepossibility, boolean ispuyosamecolor, int nextfirstpuyo, int nextsecondpuyo, boolean startfrom2rensa) {
		nrensa = -1;
		Calc(field, ojamafield,thinknextactions, thinkfirepossibility, ispuyosamecolor, nextfirstpuyo, nextsecondpuyo);
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
	
	public long[] FallDownPuyo(long puyo, long ojama, long eraseplace) {
		while (eraseplace != 0) {
			long temperase = eraseplace & (-eraseplace);
			long moveplace = -temperase;
			long saveplace = ~moveplace;
			long mv = puyo & moveplace;
			long mvo = ojama & moveplace;
			mv &= mv - 1;
			mvo &= mvo - 1;
			puyo = (mv >>> 5) | (puyo & saveplace);
			ojama = (mvo >>> 5) | (ojama & saveplace);
			eraseplace ^= temperase;
			eraseplace >>>= 5;
		}
		long[] output = {puyo, ojama};
		return output;
	}
	
	
}
