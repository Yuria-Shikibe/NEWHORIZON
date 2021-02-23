package newhorizon.bullets;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Bullet;
import newhorizon.effects.EffectTrail;

public class NHTrailBulletType extends SpeedUpBulletType {
	public int trailLength = -1;
	public float trailWidth = -1f;

	public NHTrailBulletType(float speed, float damage, String bulletSprite){
		super(speed, damage, bulletSprite);
		this.despawnEffect = Fx.none;
    }
    
    @Override
	public void init(){
		super.init();
		initTrail();
    }
    
    public void initTrail(){
	    if(trailLength < 0)trailLength = 12;
	    drawSize = Math.max(drawSize, 1.5f * trailLength * (speed + velocityEnd));
	    if(trailWidth < 0)trailWidth = width / 6f;
    }
    
	public NHTrailBulletType(float speed, float damage){
		this(speed, damage, "bullet");
    }

	public NHTrailBulletType(){
		this(1f, 1f, "bullet");
    }
    
    @Override
	public void despawned(Bullet b){
		despawnEffect.at(b.x, b.y, b.rotation(), hitColor);
		Effect.shake(despawnShake, despawnShake, b);
		hit(b);
	}
	
	@Override
	public void init(Bullet b) {
		super.init(b);
		b.data(new EffectTrail(trailLength, trailWidth));
		EffectTrail t = (EffectTrail)b.data;
		t.clear();
	}

	@Override
	public void hit(Bullet b) {
		super.hit(b);
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail t = (EffectTrail)b.data;
		t.disappear(trailColor);
	}

	@Override
	public void draw(Bullet b) {
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail t = (EffectTrail)b.data;
		t.draw(trailColor);
		super.draw(b);
	}

	@Override
	public void update(Bullet b) {
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail trail = (EffectTrail)b.data;
		if(b.timer(3, Mathf.clamp(1 / Time.delta, 0, 1))){
			trail.update(b.x + b.vel.x / 2, b.y + b.vel.y / 2);
		}
		super.update(b);
		
		
	}
}














