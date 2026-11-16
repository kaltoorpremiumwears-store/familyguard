import { IsNotEmpty, IsOptional, IsString, IsUrl } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class AddYouTubeItemDto {
  @ApiProperty({ example: 'CHANNEL', enum: ['CHANNEL', 'VIDEO'] })
  @IsString()
  @IsNotEmpty()
  type: string;

  @ApiProperty({ example: 'UCx7csxW4mS_rC1wGgR_9s0w', description: 'YouTube Channel ID or 11-char Video ID' })
  @IsString()
  @IsNotEmpty()
  externalId: string;

  @ApiProperty({ example: 'National Geographic Kids' })
  @IsString()
  @IsNotEmpty()
  title: string;

  @ApiProperty({ example: 'Educational science and animal exploration', required: false })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ example: 'https://images.unsplash.com/natgeo-kids.png', required: false })
  @IsOptional()
  @IsString()
  thumbnailUrl?: string;
}
