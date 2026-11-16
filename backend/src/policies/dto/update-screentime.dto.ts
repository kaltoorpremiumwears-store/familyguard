import { IsInt, IsOptional, IsString, IsBoolean, Min, Max } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateScreenTimeDto {
  @ApiProperty({ example: 120, description: 'Daily screen time limit in minutes' })
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(1440)
  dailyLimitMinutes?: number;

  @ApiProperty({ example: 180, description: 'Weekend screen time limit in minutes' })
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(1440)
  weekendLimitMinutes?: number;

  @ApiProperty({ example: '08:30', description: 'School time start (HH:mm)' })
  @IsOptional()
  @IsString()
  schoolStart?: string;

  @ApiProperty({ example: '15:00', description: 'School time end (HH:mm)' })
  @IsOptional()
  @IsString()
  schoolEnd?: string;

  @ApiProperty({ example: '21:00', description: 'Bedtime curfew start (HH:mm)' })
  @IsOptional()
  @IsString()
  bedtimeStart?: string;

  @ApiProperty({ example: '07:00', description: 'Bedtime curfew end (HH:mm)' })
  @IsOptional()
  @IsString()
  bedtimeEnd?: string;

  @ApiProperty({ example: false, description: 'Instant remote device pause toggle' })
  @IsOptional()
  @IsBoolean()
  isInstantPause?: boolean;

  @ApiProperty({ example: 15, description: 'Bonus screen time granted today in minutes' })
  @IsOptional()
  @IsInt()
  @Min(0)
  bonusMinutesToday?: number;
}
