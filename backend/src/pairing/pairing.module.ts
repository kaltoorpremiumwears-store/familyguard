import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PairingService } from './pairing.service';
import { PairingController } from './pairing.controller';

@Module({
  imports: [
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'super-secure-parental-ecosystem-jwt-secret-key-change-in-production-2026',
      signOptions: { expiresIn: '365d' },
    }),
  ],
  controllers: [PairingController],
  providers: [PairingService],
  exports: [PairingService],
})
export class PairingModule {}
