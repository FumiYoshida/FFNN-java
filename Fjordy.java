package player;

import java.util.HashMap;
import java.util.Map;

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


public class Fjordy extends AbstractSamplePlayer {


	
	
	@Override
	public Action doMyTurn() {
		/**
		 * ぷよを置く場所
		 */
		int columnNum = 5;
		/**
		 * 縦にぷよを配置する命令
		 */
		Field myfield = getMyBoard().getField();
		System.out.println(myfield.getHeight());
		System.out.println(myfield.getWidth());
		System.out.println(myfield.getDeadLine());
		while (columnNum >= 0) {
			if (myfield.getTop(columnNum) < 10) {
				break;
			}
			columnNum--;
		}
		System.out.print("myscore: " + getMyPlayerInfo().getOjamaScore() + "  ");
		System.out.println(getMyBoard().getNumbersOfOjamaList());
		System.out.print("enemyscore: " + getEnemyPlayerInfo().getOjamaScore() + "  ");
		System.out.println(getEnemyBoard().getNumbersOfOjamaList());
		Action action = new Action(PuyoDirection.UP,  columnNum);
		
		long start = System.currentTimeMillis();
		double temp = 0;
		for (int i=0;i<1000;i++) {
			for (int j=0;j<1000;j++) {
				for (int k=0;k<100;k++) {
					temp = (double)k * i / j / 1000;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println(end - start);
		
		return action;
	}
	
	public double[][][][] getDataTree(){
		// 自分の行動最大22通り、相手の行動最大22通り、ネクネクの次のぷよ15通り（同じ：5通り、異なる：10通り）で分岐
		// それぞれ独立なので共通部分はコピーする
		// まず自分と相手がどんな行動をとれるかを考える
		Field myfield = getMyBoard().getField();
		Field enemyfield = getEnemyBoard().getField();
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
		Puyo curpuyo = getMyBoard().getCurrentPuyo();
		PuyoType firstpuyo = curpuyo.getPuyoType(PuyoNumber.FIRST);
		PuyoType secondpuyo = curpuyo.getPuyoType(PuyoNumber.SECOND);
		Puyo[] puyos = MakePuyos(curpuyo);
		double[][] myfields;
		double[][] enemyfields;
		int myactionserial = 0;
		int enemyactionserial = 0;
		if (firstpuyo == secondpuyo) {
			myfields = new double[myactionnum][];
			enemyfields  = new double[enemyactionnum][];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					myfields[myactionserial] = getDatafromField(myfield.getNextField(puyos[0], i));
					myactionserial++;
				}
				if (enemyactions[i]) {
					enemyfields[enemyactionserial] = getDatafromField(enemyfield.getNextField(puyos[0], i));
					enemyactionserial++;
				}
			}
			for (int i=0;i<5;i++) {
				if (myactions[i+6]) {
					myfields[myactionserial] = getDatafromField(myfield.getNextField(puyos[1], i));
					myactionserial++;
				}
				if (enemyactions[i+6]) {
					enemyfields[enemyactionserial] = getDatafromField(enemyfield.getNextField(puyos[1], i));
					enemyactionserial++;
				}
			}
		}
		else {
			// 設置するぷよの向きが2通り
			myfields = new double[myactionnum*2][];
			enemyfields = new double[enemyactionnum*2][];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					myfields[myactionserial] = getDatafromField(myfield.getNextField(puyos[0], i));
					myfields[myactionserial+1] = getDatafromField(myfield.getNextField(puyos[2], i));
					myactionserial += 2;
				}
				if (enemyactions[i]) {
					enemyfields[enemyactionserial] = getDatafromField(enemyfield.getNextField(puyos[0], i));
					enemyfields[enemyactionserial+1] = getDatafromField(enemyfield.getNextField(puyos[2], i));
					enemyactionserial += 2;
				}
			}
			for (int i=0;i<5;i++) {
				if (myactions[i+6]) {
					myfields[myactionserial] = getDatafromField(myfield.getNextField(puyos[1], i));
					myfields[myactionserial+1] = getDatafromField(myfield.getNextField(puyos[3], i+1));
					myactionserial += 2;
				}
				if (enemyactions[i+6]) {
					enemyfields[enemyactionserial] = getDatafromField(enemyfield.getNextField(puyos[1], i));
					enemyfields[enemyactionserial] = getDatafromField(enemyfield.getNextField(puyos[3], i+1));
					enemyactionserial += 2;
				}
			}
		}
		return null;
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
		uepuyo.setDirection(PuyoDirection.DOWN);
		Puyo hidaripuyo = new Puyo(puyotypesmap);
		migipuyo.setDirection(PuyoDirection.LEFT);
		Puyo[] output = new Puyo[] {uepuyo, migipuyo, shitapuyo, hidaripuyo};
		return output;
	}
	
	public double[] getDatafromField(Field field) {
		double[] output = new double[432];
		int serial = 0;
		for (int i=0;i<6;i++) {
			for (int j=0;j<12;j++) {
				EnterPuyoType(field.getPuyoType(i, j), output, serial);
				serial += 6;
			}
		}
		return output;
	}
	
	public double[] getData() {
		Field myfield = getMyBoard().getField();
		Field enemyfield = getEnemyBoard().getField();
		Map<PuyoNumber, PuyoType> mynext = getMyBoard().getNextPuyo().getPuyoTypesMap();
		Map<PuyoNumber, PuyoType> mynextnext = getMyBoard().getNextNextPuyo().getPuyoTypesMap();
		Map<PuyoNumber, PuyoType> enemynext = getEnemyBoard().getNextPuyo().getPuyoTypesMap();
		Map<PuyoNumber, PuyoType> enemynextnext = getEnemyBoard().getNextNextPuyo().getPuyoTypesMap();
		int myojama = getMyPlayerInfo().getOjamaScore();
		int enemyojama = getEnemyPlayerInfo().getOjamaScore();
		double[] output = new double[906];
		int serial = 0;
		for (int i=0;i<6;i++) {
			for (int j=0;j<12;j++) {
				EnterPuyoType(myfield.getPuyoType(i, j), output, serial);
				serial += 6;
			}
		}
		for (int i=0;i<6;i++) {
			for (int j=0;j<12;j++) {
				EnterPuyoType(enemyfield.getPuyoType(i, j), output, serial);
				serial += 6;
			}
		}
		EnterPuyoTypeExceptOjama(mynext.get(Puyo.PuyoNumber.FIRST), output, 864);
		EnterPuyoTypeExceptOjama(mynext.get(Puyo.PuyoNumber.SECOND), output, 869);
		EnterPuyoTypeExceptOjama(mynextnext.get(Puyo.PuyoNumber.FIRST), output, 874);
		EnterPuyoTypeExceptOjama(mynextnext.get(Puyo.PuyoNumber.SECOND), output, 879);
		EnterPuyoTypeExceptOjama(enemynext.get(Puyo.PuyoNumber.FIRST), output, 884);
		EnterPuyoTypeExceptOjama(enemynext.get(Puyo.PuyoNumber.SECOND), output, 889);
		EnterPuyoTypeExceptOjama(enemynextnext.get(Puyo.PuyoNumber.FIRST), output, 894);
		EnterPuyoTypeExceptOjama(enemynextnext.get(Puyo.PuyoNumber.SECOND), output, 899);
		output[904] = myojama;
		output[905] = enemyojama;
		return output;
	}

	public void EnterPuyoType(PuyoType input, double[] output, int index) {
		if (input != null) {
			switch (input) {
			case BLUE_PUYO:
				output[index] = 1;
				break;
			case GREEN_PUYO:
				output[index+1] = 1;
				break;
			case OJAMA_PUYO:
				output[index+2] = 1;
				break;
			case PURPLE_PUYO:
				output[index+3] = 1;
				break;
			case RED_PUYO:
				output[index+4] = 1;
				break;
			case YELLOW_PUYO:
				output[index+5] = 1;
				break;
			}
		}
	}

	public void EnterPuyoTypeExceptOjama(PuyoType input, double[] output, int index) {
		switch (input) {
		case BLUE_PUYO:
			output[index] = 1;
			break;
		case GREEN_PUYO:
			output[index+1] = 1;
			break;
		case PURPLE_PUYO:
			output[index+2] = 1;
			break;
		case RED_PUYO:
			output[index+3] = 1;
			break;
		case YELLOW_PUYO:
			output[index+4] = 1;
			break;
		}
	}
	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new Fjordy();

		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
