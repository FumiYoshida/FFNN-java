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

public class FieldInfo {
	// ビット演算で使うフィールドの情報を保持しておくクラス
	public long[] beforefield;
	public long[] beforeojama;
	public long[] afterfield;
	public long[] afterojama;
	public long[][] fieldafteravailableactions;
	public int[][] availableactions; 
	// availableactions[i][0] には(i+1)番目の可能な行動におけるぷよをrotateする回数が、
	// availableactions[i][1] には(i+1)番目の可能な行動におけるぷよを置く位置(columnnum)が入る。
	
	public int score;
	public int nrensa;
	public double firepossibility;
	public int numtofire;
	public List<int[]> scorepotentials; 
	
	public int[] piecetofire; // それぞれの色について、いくつあれば発火できるか
	public boolean[][] topinfo; // それぞれの色について、一番少ない数で発火するためにはどの列に置けばよいか
	
	public boolean[][] placetofire1;
	public boolean[][] placetofire2;
	
	public int firstpuyo;
	public int secondpuyo;
	
	public FieldInfo() {
		// AvailableFields 、ReadField で呼ばれるコンストラクタ
		beforefield = new long[6];
		beforeojama = new long[6];
		afterfield = new long[6];
		afterojama = new long[6];
		score = 0;
		nrensa = 1;
	}
	
	public FieldInfo(long[] as, long[] bs, long[] cs, long[] ds, long[] es, long[] fs) {
		// ThinkFirePossibility で呼ばれるコンストラクタ
		long[] tempfield = {as[0], bs[0], cs[0], ds[0], es[0], fs[0]};
		long[] tempojama = {as[1], bs[1], cs[1], ds[1], es[1], fs[1]};
		beforefield = tempfield;
		beforeojama = tempojama;
		score = 0;
		nrensa = 2;
	}
	
	public FieldInfo(long a, long b, long c, long d, long e, long f, long ojamaa, long ojamab, long ojamac, long ojamad, long ojamae, long ojamaf) {
		// ThinkFirePossibility で呼ばれるコンストラクタ
		long[] tempfield = {a, b, c, d, e, f};
		long[] tempojama = {ojamaa, ojamab, ojamac, ojamad, ojamae, ojamaf};
		beforefield = tempfield;
		beforeojama = tempojama;
		score = 0;
		nrensa = 2;
	}
	
	public FieldInfo(FieldInfo field, int first, int second) {
		// Tsumos で呼ばれるコンストラクタ
		afterfield = new long[6];
		afterojama = new long[6];
		score = 0;
		nrensa = 1;
		firstpuyo = first;
		secondpuyo = second;
		for (int i=0;i<6;i++) {
			afterfield[i] = field.afterfield[i];
			afterojama[i] = field.afterojama[i];
		}
	}
	
	public FieldInfo(FieldInfo field, int first, int second, int firstcolumn, int secondcolumn, int[] top, boolean isup) {
		beforefield = Arrays.copyOf(field.beforefield, 6);
		beforeojama = Arrays.copyOf(field.beforeojama, 6);
		if (firstcolumn == secondcolumn) {

			if (isup) {
				beforefield[firstcolumn] |= (long)1 << (first + top[firstcolumn]); 
				beforefield[secondcolumn] |= (long)1 << (second + top[secondcolumn] + 5); 
			}
			else {
				beforefield[firstcolumn] |= (long)1 << (first + top[firstcolumn] + 5); 
				beforefield[secondcolumn] |= (long)1 << (second + top[secondcolumn]); 
			}
		}
		else {
			beforefield[firstcolumn] |= (long)1 << (first + top[firstcolumn]); 
			beforefield[secondcolumn] |= (long)1 << (second + top[secondcolumn]); 
		}
		score = 0;
		nrensa = 1;
	}
	
	public FieldInfo[] AvailableFields(int nextfirstpuyo, int nextsecondpuyo) {
		FieldInfo[] output = new FieldInfo[fieldafteravailableactions.length];
		for (int i=0;i<fieldafteravailableactions.length;i++) {
			output[i] = new FieldInfo();
			for (int j=0;j<6;j++) {
				output[i].beforefield = Arrays.copyOf(fieldafteravailableactions[i], fieldafteravailableactions[i].length);
				output[i].beforeojama = Arrays.copyOf(afterojama, afterojama.length);
				output[i].firstpuyo = nextfirstpuyo;
				output[i].secondpuyo = nextsecondpuyo;
			}
		}
		return output;
	}
	
	public FieldInfo[] AvailableFields() {
		FieldInfo[] output = new FieldInfo[fieldafteravailableactions.length];
		for (int i=0;i<fieldafteravailableactions.length;i++) {
			output[i] = new FieldInfo();
			for (int j=0;j<6;j++) {
				output[i].beforefield = Arrays.copyOf(fieldafteravailableactions[i], fieldafteravailableactions[i].length);
				output[i].beforeojama = Arrays.copyOf(afterojama, afterojama.length);
			}
		}
		return output;
	}
	
	
}
