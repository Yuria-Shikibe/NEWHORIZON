package newhorizon.func;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import newhorizon.bullets.EffectBulletType;
import newhorizon.content.NHFx;

import static arc.math.Angles.randLenVectors;
import static mindustry.Vars.*;
import static mindustry.core.World.toTile;

public class Functions {
    private static Tile tileParma;
    private static Floor floorParma;
    private static final Seq<Tile> tiles = new Seq<>();
    private static final IntSeq buildingIDSeq = new IntSeq();
    private final int maxCompute = 32;
    
    
    public static Seq<Tile> getAcceptableTiles(int x, int y, int range, Boolf<Tile> bool){
        Seq<Tile> tiles = new Seq<>(true, (int)(Mathf.pow(range, 2) * Mathf.pi), Tile.class);
        Geometry.circle(x, y, range, (x1, y1) -> {
            if((tileParma = world.tile(x1, y1)) != null && bool.get(tileParma)){
                tiles.add(world.tile(x1, y1));
            }
        });
        return tiles;
    }
    
    private static final Effect debugEffect = new Effect(120f, 300f, e -> {
        if(!(e.data instanceof Seq))return;
        Seq<Rect> data = e.data();
        Draw.color(Pal.lancerLaser);
        Draw.z(Layer.flyingUnit + 2f);
        for(Rect r : data){
            r.getCenter(Tmp.v1);
            Fill.square(Tmp.v1.x, Tmp.v1.y, tilesize / 2f);
        }
    });
    
    private static void clearTmp(){
        tileParma = null;
        floorParma = null;
        buildingIDSeq.clear();
        tiles.clear();
    }
    
    public static Color getColor(Color defaultColor, Team team){
        return defaultColor == null ? team.color : defaultColor;
    }
    
    public static void spawnUnit(UnitType type, Team team, int spawnNum, float x, float y){
        for(int spawned = 0; spawned < spawnNum; spawned++){
            Time.run(spawned * Time.delta, () -> {
                Unit unit = type.create(team);
                if(unit != null){
                    unit.set(x, y);
                    unit.add();
                }else Log.info("Unit == null");
            });
        }
    }
    
    public static float regSize(UnitType type){
        return type.hitSize / tilesize / tilesize / 3.25f;
    }
    
    public static boolean spawnUnit(Teamc starter, float x, float y, int spawns, float level, float spawnRange, float spawnReloadTime, float spawnDelay, float inComeVelocity, UnitType type, Color spawnColor){
        clearTmp();
        Seq<Vec2> vectorSeq = new Seq<>();
        
        Seq<Tile> tSeq = new Seq<>(Tile.class);

        tSeq.addAll(getAcceptableTiles(toTile(x), toTile(y), toTile(spawnRange),
            tile -> !tile.floor().isDeep() && !tile.cblock().solid && !tile.floor().solid && !tile.overlay().solid && !tile.block().solidifes)
        ).shuffle();
        
        if(!type.flying){
            for(int i = 0; i < spawns; i++){
                Tile[] positions = tSeq.shrink();
                if(positions.length < spawns)return false;
                vectorSeq.add(new Vec2().set(positions[i]));
            }
        }else{
            randLenVectors((long)Time.time, spawns, spawnRange, (sx, sy) -> vectorSeq.add(new Vec2(sx, sy).add(x, y)));
        }
        
        float angle, regSize = regSize(type);
        final TextureRegion pointerRegion = Core.atlas.find("new-horizon-jump-gate-pointer"), arrowRegion = Core.atlas.find("new-horizon-jump-gate-arrow");
        angle = starter.angleTo(x, y);
    
        ui.showInfoPopup("[accent]<<Caution>>[]: Level [accent]" + level + "[] fleet in coming at [" + TableFuncs.format(x / tilesize) + ", " + TableFuncs.format(y / tilesize) + "].", spawnReloadTime / 60f, 0, 20, 20, 20, 20);
        if(NHSetting.getBool("@active.debug")){
            Seq<Rect> debugSeq = new Seq<>();
            for(Tile tile : tSeq){
                debugSeq.add(tile.getBounds(new Rect()));
            }
            debugEffect.at(x, y, 0, debugSeq);
        }
        
        int i = 0;
        for (Vec2 s : vectorSeq) {
            new EffectBulletType(spawnReloadTime + i * spawnDelay){
                @Override
                public void init(Bullet b){
                    NHFx.spawnWave.at(b.x, b.y, spawnRange, spawnColor);
                }

                @Override
                public void draw(Bullet b){
                    Draw.color(spawnColor);
                    for(int i = 0; i < 4; i++){
                        float sin = Mathf.absin(Time.time, 16f, tilesize);
                        float length = (tilesize * level + sin) * b.fout() + tilesize * 2f;
                        float signSize = regSize + 0.75f + Mathf.absin(Time.time + 8f, 8f, 0.15f);
                        Tmp.v1.trns(i * 90, -length);
                        Draw.rect(pointerRegion, b.x + Tmp.v1.x,b.y + Tmp.v1.y, pointerRegion.width * Draw.scl * signSize, pointerRegion.height * Draw.scl * signSize, i * 90 - 90);
                    }

                    for (int i = 0; i <= 8; i++) {
                        Tmp.v1.trns(angle, (i - 4) * tilesize * 2);
                        float f = (100 - (Time.time - 12.5f * i) % 100) / 100;
                        Draw.rect(arrowRegion, b.x + Tmp.v1.x, b.y + Tmp.v1.y, pointerRegion.width * (regSize / 2f + Draw.scl) * f, pointerRegion.height * (regSize / 2f + Draw.scl) * f, angle - 90);
                    }

                    Draw.reset();
                }

                @Override
                public void despawned(Bullet b) {
                    NHFx.spawn.at(b.x, b.y, regSize, spawnColor, starter);
                }

            }.create(starter, s.x, s.y, angle);

            Time.run(spawnReloadTime + i * spawnDelay, () -> {
                if(!Units.canCreate(starter.team(), type))return;
                Unit unit = type.create(starter.team());
                    unit.set(s.x, s.y);
                unit.rotation = angle;
                if(type.flying){
                    NHFx.jumpTrail.at(unit.x, unit.y, angle, spawnColor, unit);
                    Tmp.v1.trns(angle, inComeVelocity).scl(type.drag + 2);
                    unit.vel.add(Tmp.v1.x, Tmp.v1.y);
                    unit.add();
                }else{
                    Fx.unitSpawn.at(unit.x, unit.y, angle, type);
                    Time.run(Fx.unitSpawn.lifetime, () -> {
                        unit.add();
                        for(int j = 0; j < 3; j++){
                            Time.run(j * 8, () -> Fx.spawn.at(unit));
                        }
                        Effect.shake(type.hitSize / 2.4f, spawnDelay * 4, unit);
                        NHFx.spawnGround.at(unit.x, unit.y, type.hitSize / tilesize * 3, spawnColor);
                        NHFx.circle.at(unit.x, unit.y, type.hitSize * 4, spawnColor);
                    });
                }
                Sounds.plasmaboom.at(unit.x, unit.y);
            });

            i++;
        }
        return true;
    }
}
