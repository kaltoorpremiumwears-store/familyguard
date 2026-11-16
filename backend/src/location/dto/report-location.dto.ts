import { IsInt, IsNotEmpty, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class ReportLocationDto {
  @ApiProperty({ example: '123e4567-e89b-12d3-a456-426614174000' })
  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @ApiProperty({ example: 37.7749 })
  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude: number;

  @ApiProperty({ example: -122.4194 })
  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude: number;

  @ApiProperty({ example: 12.5, description: 'GPS Accuracy radius in meters' })
  @IsOptional()
  @IsNumber()
  accuracyMeters?: number;

  @ApiProperty({ example: 84, description: 'Current battery percentage' })
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(100)
  batteryPercentage?: number;
}
