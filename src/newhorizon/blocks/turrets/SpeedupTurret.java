package newhorizon.blocks.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.entities.bullet.*;

import newhorizon.colors.*;

public class SpeedupTurret extends PowerTurret{
	public float maxSpeedupScl = 0.5f;
	public float speedupPerShoot = 0.075f;
	public float slowDownReloadTime = 45f;
	
	public SpeedupTurret(String name){
		super(name);
	}
	
	@Override
	public void setBars(){
		super.setBars();
		bars.add("liquid", 
			(SpeedupTurretBuild entity) -> new Bar(
				() -> "Speed Up:",
				() -> NHColor.lightSky,
				() -> entity.speedupScl / maxSpeedupScl
			)
		);
	}
	
	public class SpeedupTurretBuild extends PowerTurretBuild{
		public float speedupScl = 0f;
		public float slowDownReload = 0f;
		
		
		@Override
		public void updateTile(){
			super.updateTile();
			if(slowDownReload >= 1f){
				slowDownReload -= Time.delta;
			}else speedupScl = Mathf.lerpDelta(speedupScl, 0f, 0.05f);
			
		}
		
		@Override
		protected void updateShooting(){
            if(reload >= reloadTime){
                BulletType type = peekAmmo();

                shoot(type);

                reload = 0f;
            }else{
                reload += (1 + speedupScl) * delta() * peekAmmo().reloadMultiplier * baseReloadSpeed();
            }
        }
				
		@Override
		protected void shoot(BulletType type){
			super.shoot(type);
			
			slowDownReload = slowDownReloadTime;
			if(speedupScl < maxSpeedupScl){
				speedupScl += speedupPerShoot;
			}else speedupScl = maxSpeedupScl;
		}
		
	}
}












