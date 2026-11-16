import { Controller, Post, Get, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { PairingService } from './pairing.service';
import { GeneratePairingDto } from './dto/generate-pairing.dto';
import { ClaimPairingDto } from './dto/claim-pairing.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Device Pairing')
@Controller('api/v1/pairing')
export class PairingController {
  constructor(private readonly pairingService: PairingService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('generate')
  @ApiOperation({ summary: 'Generate time-limited 6-char pairing code and QR payload' })
  @ApiResponse({ status: 201, description: 'Pairing invitation generated' })
  async generate(@Request() req, @Body() dto: GeneratePairingDto) {
    return this.pairingService.generatePairing(req.user.parentId, dto);
  }

  @Post('claim')
  @ApiOperation({ summary: 'Child device claims pairing with 6-char code or QR token' })
  @ApiResponse({ status: 201, description: 'Device paired successfully; returns device access token' })
  @ApiResponse({ status: 400, description: 'Code expired or invalid' })
  async claim(@Body() dto: ClaimPairingDto) {
    return this.pairingService.claimPairing(dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('status/:pairingId')
  @ApiOperation({ summary: 'Parent polls/checks whether child device has completed pairing' })
  async getStatus(@Request() req, @Param('pairingId') pairingId: string) {
    return this.pairingService.getPairingStatus(req.user.parentId, pairingId);
  }
}
