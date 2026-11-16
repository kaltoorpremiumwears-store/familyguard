import { IsArray, IsInt, IsNotEmpty, IsString, Min, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty } from '@nestjs/swagger';

export class AppUsageItemDto {
  @ApiProperty({ example: 'com.roblox.client' })
  @IsString()
  @IsNotEmpty()
  packageName: string;

  @ApiProperty({ example: 'Roblox' })
  @IsString()
  @IsNotEmpty()
  appName: string;

  @ApiProperty({ example: 'GAMES' })
  @IsString()
  @IsNotEmpty()
  category: string;

  @ApiProperty({ example: 1800, description: 'Duration in foreground in seconds' })
  @IsInt()
  @Min(0)
  durationSeconds: number;

  @ApiProperty({ example: '2026-11-14', description: 'Date in YYYY-MM-DD' })
  @IsString()
  @IsNotEmpty()
  recordedDate: string;
}

export class ReportUsageDto {
  @ApiProperty({ example: '123e4567-e89b-12d3-a456-426614174000' })
  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @ApiProperty({ type: [AppUsageItemDto] })
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => AppUsageItemDto)
  records: AppUsageItemDto[];
}
