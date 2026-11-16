import { IsArray, IsNotEmpty, IsOptional, IsString, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty } from '@nestjs/swagger';

export class InstalledAppItemDto {
  @ApiProperty({ example: 'com.mojang.minecraftpe' })
  @IsString()
  @IsNotEmpty()
  packageName: string;

  @ApiProperty({ example: 'Minecraft' })
  @IsString()
  @IsNotEmpty()
  appName: string;

  @ApiProperty({ example: 'GAMES', required: false })
  @IsOptional()
  @IsString()
  category?: string;
}

export class SyncInstalledAppsDto {
  @ApiProperty({ type: [InstalledAppItemDto] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => InstalledAppItemDto)
  apps: InstalledAppItemDto[];
}
