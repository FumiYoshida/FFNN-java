package player;

import hira.player.lib5ultimate.HiraLib5Ultimate;
import SamplePlayer.Nohoho;
import UsuiPlayer.UsuiPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;
import jp.ac.u_tokyo.torilab.usui.UsuiPlayerLv2.UsuiPlayerLv2;
import jp.ac.u_tokyo.torilab.usui.usuiPlayerLv3.UsuiPlayerLv3;
import maou2014.Maou;
import moc.liamtoh900ognek.KajiGodGod;
import player.Fjordy;
import player.UrusaiPlayer;
import player.SamplePlayer01;
/**
 * 任意の二体のエージェント同士を対戦させるためのクラス
 */
public class VSMode {

	public static void main(String args[]) {

		AbstractPlayer Nohoho = new Nohoho("Nohoho");	//カエル積み
		AbstractPlayer TA1 = new UsuiPlayer("UsuiLV1");		//TA1
		AbstractPlayer TA2 = new UsuiPlayerLv2("UsuiLV3");		//TA2
		AbstractPlayer TA3 = new UsuiPlayerLv3("UsuiLV3");		//TA3
		AbstractPlayer TA4 = new KajiGodGod("KajiGod");	//TA4
		AbstractPlayer maou = new Maou("Maou");			//かつての一位
		AbstractPlayer hiraUltimate = new HiraLib5Ultimate("Ultimate");	//2015年度優勝
		AbstractPlayer fjordy = new Fjordy("C:\\Users\\sakur\\Documents\\java\\scoredqn\\Mon_Apr_22_09_06_21_JST_2019");
		AbstractPlayer otameshi = new SamplePlayer01();
		//AbstractPlayer player = 自分の作ったプレイヤー;

		/**
		 * 任意の二つのエージェントを対戦させる．<br>
		 */
		PuyoPuyo puyopuyo = new PuyoPuyo(fjordy, otameshi);
		puyopuyo.puyoPuyo();



	}
}
