import { IsBoolean, IsInt, IsOptional, IsString, Min, Max } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateWebSafetyDto {
  @ApiProperty({ example: true, description: 'Force SafeSearch on Google, Bing, Yahoo' })
  @IsOptional()
  @IsBoolean()
  safeSearchEnabled?: boolean;

  @ApiProperty({ example: true, description: 'Block known adult & pornographic domains' })
  @IsOptional()
  @IsBoolean()
  blockAdultContent?: boolean;

  @ApiProperty({ example: true, description: 'Block gambling & betting domains' })
  @IsOptional()
  @IsBoolean()
  blockGambling?: boolean;

  @ApiProperty({ example: true, description: 'Block violence & weapon domains' })
  @IsOptional()
  @IsBoolean()
  blockViolence?: boolean;

  @ApiProperty({ example: true, description: 'Block malware & phishing domains' })
  @IsOptional()
  @IsBoolean()
  blockMalicious?: boolean;

  @ApiProperty({ example: true, description: 'Suspend unmanaged browsers that bypass Chrome controls' })
  @IsOptional()
  @IsBoolean()
  unapprovedBrowsersBlocked?: boolean;

  @ApiProperty({ example: 'SAFETUBE', enum: ['SAFETUBE', 'MANAGED_CHROME'] })
  @IsOptional()
  @IsString()
  youtubeMode?: string;

  @ApiProperty({ example: 60, description: 'Daily YouTube allowance in minutes' })
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(720)
  dailyYouTubeLimitMinutes?: number;
}
