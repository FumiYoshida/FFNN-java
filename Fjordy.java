package player;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
	
	ScoreQLearning dqn;
	boolean isstart = true;
	int turnnum = 0;
	static String dirname = "";
	int honki = 50;
	List<Double> errors;
	
	public Fjordy(String directoryname){
		dirname = directoryname;
	}
	
	@Override
	public Action doMyTurn() {
		if (isstart) {
			dqn = new ScoreQLearning();
			dqn.FFNNSettings(getMyBoard());
			if (dirname != "") {
				dqn.nn.ReadLearned(dirname);
			}
			dqn.nn.usermsprop = true;
			errors = new ArrayList<Double>();
			isstart = false;
		}
		turnnum++;
		if (honki < -450) {
			System.out.println("いまから50ターン");
			dqn.epsilon = 0;
			honki = 50;
		}
		else {
			if (honki > 0) {
				dqn.epsilon = 0;
				honki--;
			}
			else {
				dqn.epsilon = 0.1; // default : 0.1
				honki--;
			}
		}
		Action myaction = dqn.Main(getMyBoard(), getMyPlayerInfo(), getEnemyBoard(), getEnemyPlayerInfo());
		errors.add(dqn.errorsum);
		if (errors.size() > 100) {
			errors.remove(0);
		}
		if (turnnum % 20 == 0) {
			double avelos = 0;
			for (double los : errors) {
				avelos += los;
			}
			avelos /= (errors.size() * 2);
			System.out.println("誤差の絶対値の平均は " + avelos + " です");
			System.out.println("現在の評価値 : " + dqn.evaluationvalue);
		}
		Date date = new Date();
		if (turnnum % 500 == 0) {
			dqn.nn.Save();
			System.out.println("turn : " + turnnum + ", saved");
		}
		return myaction;
	}
	
	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new Fjordy(dirname);
		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
