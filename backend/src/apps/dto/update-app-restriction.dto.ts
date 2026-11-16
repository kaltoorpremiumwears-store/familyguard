import { IsEnum, IsInt, IsNotEmpty, IsOptional, IsString, Min } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateAppRestrictionDto {
  @ApiProperty({ example: 'com.roblox.client' })
  @IsString()
  @IsNotEmpty()
  packageName: string;

  @ApiProperty({ example: 'Roblox' })
  @IsString()
  @IsNotEmpty()
  appName: string;

  @ApiProperty({ example: 'GAMES', enum: ['GAMES', 'SOCIAL', 'EDUCATION', 'ENTERTAINMENT', 'UTILITY'] })
  @IsString()
  @IsNotEmpty()
  category: string;

  @ApiProperty({ example: 'BLOCKED', enum: ['ALLOWED', 'BLOCKED', 'TIME_LIMITED'] })
  @IsString()
  @IsNotEmpty()
  mode: string;

  @ApiProperty({ example: 45, description: 'Daily limit in minutes when mode is TIME_LIMITED' })
  @IsOptional()
  @IsInt()
  @Min(0)
  dailyLimitMinutes?: number;
}

export class BulkUpdateAppRestrictionsDto {
  @ApiProperty({ type: [UpdateAppRestrictionDto] })
  restrictions: UpdateAppRestrictionDto[];
}
