import { IsBoolean, IsNotEmpty, IsOptional, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AddWebsiteRuleDto {
  @ApiProperty({ example: 'discord.com' })
  @IsString()
  @IsNotEmpty()
  domain: string;

  @ApiProperty({ example: true, description: 'true to block, false to whitelist' })
  @IsBoolean()
  isBlocked: boolean;

  @ApiProperty({ example: 'CUSTOM', enum: ['ADULT', 'GAMBLING', 'MALICIOUS', 'CUSTOM'] })
  @IsOptional()
  @IsString()
  category?: string;
}
