package space.bbkr.creeperworks.mixins;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(EntityCreeper.class)
public abstract class MixinEntityCreeper extends EntityLivingBase {

    @Shadow protected abstract void spawnLingeringCloud();

    @Shadow public abstract boolean getPowered();

    private  MixinEntityCreeper(EntityType type, World world) {
        super(type, world);
    }

    @Inject(method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZ)Lnet/minecraft/world/Explosion;"),
            cancellable = true)
    public void creeperDamage(CallbackInfo ci) {
        Vec3d pos = new Vec3d(this.posX, this.posY, this.posZ);
        double range = this.getPowered()? 15 : 5;
        List<EntityLivingBase> entities = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(range));

        for (EntityLivingBase entity : entities) {
            if (this.getDistanceSq(entity) <= 25.0D && entity != this) {
                int multiplier = this.getPowered()? 20 : 15;
                float damage = multiplier * (float) Math.sqrt((range - (double) this.getDistance(entity)) / range);
                entity.attackEntityFrom(DamageSource.causeExplosionDamage(this), damage);
            }
        }
        this.setDead();
        this.spawnLingeringCloud();
        ci.cancel();
    }

    @Inject(method = "explode", at = @At(value = "HEAD"))
    public void creeperFirework(CallbackInfo ci) {
        if (this.world.isRemote) {
            NBTTagCompound creeperWork = new NBTTagCompound();
            NBTTagCompound fireworkDesign = new NBTTagCompound();
            NBTTagCompound bigBall = new NBTTagCompound();
            if (!this.getPowered()) {
                fireworkDesign.setInteger("Type", 2);
                fireworkDesign.setIntArray("Colors", new int[]{4312372});
                fireworkDesign.setIntArray("FadeColors", new int[]{1973019});
            } else {
                fireworkDesign.setInteger("Type", 3);
                fireworkDesign.setIntArray("Colors", new int[]{2651799});
                fireworkDesign.setIntArray("FadeColors", new int[]{15790320});
                bigBall.setInteger("Type", 1);
                bigBall.setIntArray("Colors", new int[]{4312372});
                bigBall.setIntArray("FadeColors", new int[]{1973019});
            }
            NBTTagList explosions = new NBTTagList();
            explosions.add(fireworkDesign);
            explosions.add(bigBall);
            creeperWork.setTag("Explosions", explosions);
            double explosionHeight = getPowered()? this.posY+2 : this.posY+1;
            this.world.makeFireworks(this.posX, explosionHeight, this.posZ, this.motionX, this.motionY, this.motionZ, creeperWork);
        }
    }
}
