package newhorizon.blocks.special;

import arc.*;
import arc.func.Cons;
import arc.math.*;
import arc.util.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.Category;
import mindustry.ui.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;

import newhorizon.content.NHFx;
import newhorizon.colors.*;
import newhorizon.func.PosLightning;

import static mindustry.Vars.*;

public class ChargeWall extends Block{
	public TextureRegion heatRegion, lightRegion;
	public float maxEnergy = size * size * 4000;
	public float maxHeat = size * size * 600;
	public float heatPerRise = 50f;
	public float healLightStMin = 0.35f;
	
	public float healReloadTime = 45f, 
				 healPerEnr = 45f, 
				 healPercent = size * size * 1.5f;
				
	public float shootReloadTime = 8f,
				 shootPerEnr = 80f,
				 shootDamage = 100f;
				
	public float coolingReloadTime = 30f;
	
	public float chargeCoefficient = 1.1f;
				
	public float range = 200f;
	public int lightningActHits = size * size * 4;
	
	public Color effectColor = NHColor.lightSky;
	public Effect
		chargeActEffect = NHFx.circleSplash,
		hitEffect = NHFx.circleSplash,
		shootEffect = NHFx.circleSplash,
		onDestroyedEffect = Fx.none;
	
	Cons<ChargeWallBuild> maxChargeAct = tile -> {
		chargeActEffect.at(tile.x, tile.y, effectColor);
		
		PosLightning.createRange(tile, tile.team(), tile.range(), lightningActHits, 2, maxEnergy, effectColor, true, PosLightning.WIDTH);
	};
	Cons<ChargeWallBuild> destroyAct = tile -> {
		onDestroyedEffect.at(tile.x, tile.y, effectColor);
		
		PosLightning.createRange(tile, tile.team(), tile.range(), lightningActHits, 2, maxEnergy, effectColor, true, PosLightning.WIDTH);
	};
	Cons<ChargeWallBuild> closestTargetAct = tile -> PosLightning.create(tile, tile.target, tile.team, effectColor, PosLightning.WIDTH, 2, target ->{
		hitEffect.at(target.getX(), target.getY(), tile.angleTo(target), effectColor);
		shootEffect.at(tile.x, tile.y, effectColor);
		new SapBulletType() {
			{
				damage = shootDamage;
				status = StatusEffects.none;
				sapStrength = 0.45f;
				length = tile.range();
				drawSize = tile.range() * 2;
				hitColor = color = effectColor;
				despawnEffect = shootEffect = Fx.none;
				width = 0.62f;
				lifetime = 35f;
			}
		}.create(tile, tile.team, tile.x, tile.y, tile.angleTo(target));
	});
	
	public ChargeWall(String name){
		super(name);
		update = true;
		solid = true;
		buildCostMultiplier = 4;
		this.category = Category.defense;
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid) {
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);
	}
	
	@Override
	public void setBars(){
		super.setBars();
		bars.add("Energy", 
			(ChargeWallBuild entity) -> new Bar(
				() -> "Energy",
				() -> NHColor.lightSky,
				() -> entity.energy / maxEnergy
			)
		);
		
		bars.add("Heat", 
			(ChargeWallBuild entity) -> new Bar(
				() -> "Heat",
				() -> Color.valueOf("#FF732A"),
				() -> entity.heat / maxHeat
			)
		);
	}
	
	@Override
	public void load(){
		super.load();
		heatRegion  = Core.atlas.find(name + "-heat");
		lightRegion = Core.atlas.find(name + "-light");
	}
	
	public class ChargeWallBuild extends Building implements Ranged{
		public float energy;
		public float healReload;
		public float dmgScl = 1;
		public float heat;
		public float shootHeat;
		public float shootReload;
		public float coolingReload;
		
		public Posc target;
		
		@Override
		public float range(){return range;}
		
		@Override
		public float handleDamage(float amount) {return amount * dmgScl;}
		
		@Override
		public boolean collision(Bullet other) {
			float dmg = other.damage() * other.type().buildingDamageMultiplier;
			this.damage(dmg);
			energy += chargeCoefficient * dmg * dmgScl;
			return true;
		}
		
		@Override
		public void updateTile() {
			coolingReload += Time.delta;
			if(coolingReload > coolingReloadTime)updateCooling();
			shootHeat = Mathf.lerpDelta(shootHeat, 0f, 0.015f);
			if (timer(0, 20)) {
				findTarget();
			}
			if (!validateTarget()) target = null;
			else if(energy > shootPerEnr)updateShooting();
			fixEnr();
			updateHealTile();
			if(energy > maxEnergy){
				maxChargeAct.get(this);
				heatRise();
			}
			
			if(heat > (maxHeat * healLightStMin)){
				if(Mathf.chance(0.15f * Time.delta)){
					Fx.reactorsmoke.at(tile.x + Mathf.range(size * tilesize / 2), tile.y + Mathf.range(size * tilesize / 2));
				}
			}
		}
		
		@Override
		public void onDestroyed() {
			super.onDestroyed();
			destroyAct.get(this);
			if(target != null)closestTargetAct.get(this);
		}
		
		protected void updateShooting(){
			coolingReload = 0f;
			if(shootReload < shootReloadTime){
				shootReload += Time.delta;
			}else{
				energy -= shootPerEnr;
				shootReload = 0f;
				shootHeat = 1f;
				closestTargetAct.get(this);
			}
		}
		
		protected void updateCooling(){
			energy = Mathf.lerpDelta(energy, 0f, 0.015f);
			heat = Mathf.lerpDelta(heat, 0f, 0.015f);
		}
		
		protected void findTarget(){
			target = Units.closestTarget(team, x, y, range());
		}
		
		protected boolean validateTarget() {
			return !Units.invalidateTarget(target, team, x, y);
		}
		
		protected void fixEnr(){if(energy < 0)energy = 0f;}
		
		protected void updateHealTile(){
			if(energy < healPerEnr)return;
			if(healReload < healReloadTime){
				healReload += Time.delta;
			}else{
				energy -= healPerEnr;
				healReload = 0f;
				Fx.healBlockFull.at(x, y, block.size, effectColor);
				if(healthf() > 0.975f)this.heat -= healPercent * maxHeat;
				this.heal(healPercent * this.maxHealth);
			}
		}
		
		protected void heatRise(){
			energy = 0f;
			if(heat < maxHeat){heat += heatPerRise;}
			else {
				onDestroyed();
				kill();
			}
		}
		
		@Override
		public void draw(){
			Draw.rect(region, x, y);
			Draw.z(Layer.bullet);
			Draw.color(effectColor);
			Draw.alpha(energy / maxEnergy * 4);
			Draw.rect(lightRegion, x, y);
			Draw.reset();
			if(heat > maxHeat * healLightStMin){
				Draw.blend(Blending.additive);
				float flash = 1f + ((heat - maxHeat * healLightStMin) / (1f - maxHeat * healLightStMin)) * 5.4f;
                flash += flash * Time.delta;
                Draw.color(Color.red, Color.yellow, Mathf.absin(flash, 9f, 1f));
                Draw.alpha(0.6f);
                Draw.rect(heatRegion, x, y);
            }
            Draw.blend();
            Draw.reset();
		}
		
		@Override
		public void drawSelect() {
			Drawf.dashCircle(x, y, range(), team.color);
		}
		
		
	}
}